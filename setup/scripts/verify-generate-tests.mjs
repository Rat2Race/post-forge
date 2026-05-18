#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(process.argv[2] || path.join(scriptDir, "../.."));
const generatorPath = path.join(repoRoot, "setup/scripts/generate-tests.mjs");
const workspace = fs.mkdtempSync(path.join(os.tmpdir(), "setup-generate-tests-"));

function writeFile(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function collectFiles(dirPath) {
  const files = [];
  if (!fs.existsSync(dirPath)) return files;

  function visit(currentPath) {
    for (const entry of fs.readdirSync(currentPath, { withFileTypes: true })) {
      const fullPath = path.join(currentPath, entry.name);
      if (entry.isDirectory()) {
        visit(fullPath);
      } else if (entry.isFile()) {
        files.push(fullPath);
      }
    }
  }

  visit(dirPath);
  return files.sort();
}

function writePolicy(projectRoot) {
  writeFile(path.join(projectRoot, "tests/testing-policy.yml"), `schemaVersion: 1
baseUrl: http://localhost:8080
endpoints:
  - method: GET
    path: /widgets
    class: smoke
    confidence: high
    reason: generated verifier sample
    reviewRequired: false
    source:
      type: verifier
`);
}

function writeJpaSample(projectRoot) {
  writeFile(path.join(projectRoot, "src/main/java/example/WidgetStatus.java"), `package example;

public enum WidgetStatus {
    ACTIVE,
    ARCHIVED
}
`);

  writeFile(path.join(projectRoot, "src/main/java/example/Widget.java"), `package example;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "widgets")
public class Widget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WidgetStatus status;
}
`);
}

function createProject(name) {
  const projectRoot = path.join(workspace, name);
  writePolicy(projectRoot);
  writeJpaSample(projectRoot);
  writeFile(path.join(projectRoot, "tests/sql/manual/keep.sql"), "-- manual SQL must not be overwritten\n");
  return projectRoot;
}

function runGenerate(projectRoot) {
  const policyPath = path.join(projectRoot, "tests/testing-policy.yml");
  const resultPath = path.join(projectRoot, "setup/state/generated-tests-result.json");
  const reportPath = path.join(projectRoot, "setup/reports/testing-setup-report.md");
  const result = spawnSync(process.execPath, [generatorPath, projectRoot, policyPath, resultPath, reportPath], {
    cwd: projectRoot,
    encoding: "utf8",
    maxBuffer: 1024 * 1024 * 10,
  });

  if (result.status !== 0) {
    throw new Error(`generate-tests failed for ${path.basename(projectRoot)}\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}`);
  }

  return JSON.parse(fs.readFileSync(resultPath, "utf8"));
}

function readRelativeContents(projectRoot, dirName) {
  const dirPath = path.join(projectRoot, dirName);
  return Object.fromEntries(
    collectFiles(dirPath).map((filePath) => [
      path.relative(projectRoot, filePath),
      fs.readFileSync(filePath, "utf8"),
    ]),
  );
}

function assertNoSqlOrSeedArtifacts(projectRoot, result) {
  assert(!Object.hasOwn(result, "fixtureConfig"), "result should not expose fixtureConfig metadata");
  assert(!Object.hasOwn(result, "sqlFixtures"), "result should not expose sqlFixtures metadata");
  assert(!Object.hasOwn(result.bruno, "authFixtureStatus"), "Bruno result should not expose auth fixture status");
  assert(!Object.hasOwn(result.bruno, "authFixtureRequests"), "Bruno result should not expose auth fixture counts");
  assert(!fs.existsSync(path.join(projectRoot, "tests/smoke/seed.sh")), "SQL execution helper should not be generated");
  assert(!fs.existsSync(path.join(projectRoot, "tests/bruno/api/generated/auth")), "seed/auth Bruno fixtures should not be generated");
}

function assertSqlDrafts(projectRoot, result) {
  assert(Object.hasOwn(result, "sqlDrafts"), "result should expose SQL draft metadata");
  assert(result.sqlDrafts.status === "generated", "SQL draft status should be generated for sample JPA entities");
  assert(result.sqlDrafts.generatedRoot === "tests/sql/generated", "SQL draft generated root should be tests/sql/generated");
  assert(result.sqlDrafts.manualRoot === "tests/sql/manual", "SQL draft manual root should be tests/sql/manual");
  assert(result.sqlDrafts.files.length === 2, "SQL draft generation should write reset and insert drafts");
  assert(fs.existsSync(path.join(projectRoot, "tests/sql/generated/001-reset-draft.sql")), "SQL reset draft should be generated");
  assert(fs.existsSync(path.join(projectRoot, "tests/sql/generated/002-jpa-insert-draft.sql")), "SQL insert draft should be generated");

  const insertSql = fs.readFileSync(path.join(projectRoot, "tests/sql/generated/002-jpa-insert-draft.sql"), "utf8");
  assert(insertSql.includes("INSERT INTO widgets"), "SQL insert draft should contain sample JPA table");
  assert(insertSql.includes("'Widget draft title'"), "SQL insert draft should contain deterministic sample values");
}

function assertSetupScaffold(projectRoot) {
  assert(fs.existsSync(path.join(projectRoot, "tests/bruno/api/manual/folder.bru")), "Bruno manual placeholder should be regenerated when tests is missing");
  assert(fs.existsSync(path.join(projectRoot, "tests/k6/manual/.gitkeep")), "k6 manual placeholder should be regenerated when tests is missing");
  assert(fs.existsSync(path.join(projectRoot, "tests/sql/manual/.gitkeep")), "SQL manual placeholder should be regenerated when tests is missing");
  assert(fs.readFileSync(path.join(projectRoot, "tests/sql/manual/keep.sql"), "utf8").includes("manual SQL"), "SQL manual files should not be overwritten");
}

function assertNoProjectSpecificText(projectRoot) {
  const generatedFiles = [
    ...collectFiles(path.join(projectRoot, "tests")),
    ...collectFiles(path.join(projectRoot, "setup/state")),
    ...collectFiles(path.join(projectRoot, "setup/reports")),
  ];
  const combined = generatedFiles.map((filePath) => fs.readFileSync(filePath, "utf8")).join("\n");
  assert(!/PostForge|postforge|dev\.iamrat|iamrat/.test(combined), "generated artifacts should not contain PostForge-specific text");
}

try {
  const projectA = createProject("project-a");
  const projectB = createProject("project-b");

  const projectAResult = runGenerate(projectA);
  const projectBResult = runGenerate(projectB);

  assertNoSqlOrSeedArtifacts(projectA, projectAResult);
  assertNoSqlOrSeedArtifacts(projectB, projectBResult);
  assertSqlDrafts(projectA, projectAResult);
  assertSqlDrafts(projectB, projectBResult);
  assertSetupScaffold(projectA);
  assertNoProjectSpecificText(projectA);

  assert(
    JSON.stringify(readRelativeContents(projectA, "tests/bruno/api/generated")) === JSON.stringify(readRelativeContents(projectB, "tests/bruno/api/generated")),
    "identical generic projects should produce identical Bruno generated artifacts",
  );
  assert(
    JSON.stringify(readRelativeContents(projectA, "tests/sql/generated")) === JSON.stringify(readRelativeContents(projectB, "tests/sql/generated")),
    "identical generic projects should produce identical SQL draft artifacts",
  );
  console.log("[ok] generate-tests artifact verification passed");
} finally {
  if (process.env.KEEP_SETUP_VERIFY_TMP === "true") {
    console.log(`[info] kept temp workspace: ${workspace}`);
  } else {
    fs.rmSync(workspace, { recursive: true, force: true });
  }
}
