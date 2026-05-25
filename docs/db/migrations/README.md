# DB Migration Convention

PostForge does not yet run Flyway or Liquibase automatically.
Until that decision is made, this directory is the repo-owned SQL review surface for schema changes.

## Naming

Use:

```text
VNNNN__short_description.sql
```

Examples:

```text
V0001__baseline_schema_ownership.sql
V0002__rename_collected_articles_source_columns.sql
```

## Required Header

Each SQL artifact starts with:

```sql
-- Owner:
-- Purpose:
-- Tables:
-- Compatibility:
-- Rollback:
-- Verification:
```

## Rules

- Update `docs/db/schema-ownership.md` in the same change when table ownership or schema changes.
- Keep one primary owner per migration.
- Mention every affected table in the header.
- Destructive changes require a compatibility and rollback note.
- New JPA entity/table changes require a matching SQL artifact, even if it is applied manually for now.
- Do not mix migration-tool adoption with broad schema redesign in the same change.

