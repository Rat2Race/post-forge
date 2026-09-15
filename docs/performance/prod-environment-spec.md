# 운영 서버 환경 기록

수집 시각: 2026-06-17 07:06:13 UTC

이 문서는 수집 시점의 운영 host와 컨테이너 상태를 보존한다. 현재 환경을 다시 확인한 기록이나 부하 테스트 결과가 아니다. 실제 수용량 판단은 새 실행과 같은 시간 구간의 k6 리포트, Prometheus 지표, 컨테이너 상태와 함께 본다.

## 호스트

| 항목 | 값 |
|---|---|
| Hostname | `prod` |
| OS | Ubuntu 22.04.5 LTS (Jammy Jellyfish) |
| Kernel | Linux 5.15.0-174-generic |
| Architecture | x86_64 |
| Chassis | desktop |

`hostnamectl`에 포함된 machine ID, boot ID 같은 장비 식별자는 문서에 남기지 않는다.

## CPU

| 항목 | 값 |
|---|---:|
| 모델 | Intel(R) N100 |
| 논리 CPU | 4 |
| Socket당 core | 4 |
| Core당 thread | 1 |
| Socket | 1 |
| 최소 클럭 | 700 MHz |
| 최대 클럭 | 3.4 GHz |
| L1d cache | 128 KiB |
| L1i cache | 256 KiB |
| L2 cache | 2 MiB |
| L3 cache | 6 MiB |
| Virtualization | VT-x |

해석: 이 host는 과거 1 vCPU / 1 GB 벤치마크 환경보다 CPU 여유가 크다. 따라서 과거 환경의 RPS 수치를 production 상한으로 그대로 보면 안 되고, 같은 부하 시나리오를 다시 실행해 새 기준선을 잡아야 한다.

## 메모리와 Swap

| 항목 | 값 |
|---|---:|
| 전체 메모리 | 15 GiB |
| 수집 시점 사용 메모리 | 1.1 GiB |
| 수집 시점 free 메모리 | 5.2 GiB |
| 수집 시점 buffer/cache | 9.1 GiB |
| 수집 시점 available 메모리 | 13 GiB |
| 전체 Swap | 4.0 GiB |
| 수집 시점 Swap 사용량 | 0 B |

해석: 수집 시점의 유휴 상태 snapshot 기준으로 app, PostgreSQL, Redis, filesystem cache를 위한 메모리 여유가 충분하다. Swap은 설정되어 있지만 사용 중이지 않았다.

## 디스크

| Mount | Type | Size | Used | Available | Use |
|---|---|---:|---:|---:|---:|
| `/` | ext4 on LVM | 98 GiB | 20 GiB | 74 GiB | 21% |
| `/boot` | ext4 | 2.0 GiB | 260 MiB | 1.6 GiB | 15% |
| `/boot/efi` | vfat | 1.1 GiB | 6.1 MiB | 1.1 GiB | 1% |

물리 디스크 layout:

| Device | Size | Type | Mount |
|---|---:|---|---|
| `sda` | 476.9 GiB | disk |  |
| `sda1` | 1 GiB | partition | `/boot/efi` |
| `sda2` | 2 GiB | partition | `/boot` |
| `sda3` | 473.9 GiB | LVM member |  |
| `ubuntu--vg-ubuntu--lv` | 100 GiB | LVM/ext4 | `/` |

해석: 수집 시점 기준 root filesystem 여유 공간은 충분하다. 물리 디스크 크기가 현재 root logical volume보다 크므로, LVM 설정에 따라 추후 확장 여지가 있을 수 있다.

## 컨테이너 런타임

| 항목 | 값 |
|---|---|
| Docker Engine | 29.3.0 |
| Docker API | 1.54 |
| containerd | 2.2.2 |
| runc | 1.3.4 |
| Docker Compose | v5.1.0 |

## 수집 시점의 컨테이너

`docker ps`와 `docker stats --no-stream` 기준.

| 컨테이너 | 이미지 | 상태 | CPU | 메모리 | PIDs | 비고 |
|---|---|---|---:|---:|---:|---|
| `postforge-app` | `rat2hub/postforge:latest` | Up 6 hours | 0.39% | 586 MiB / 15.4 GiB | 46 | port `8080` published |
| `postforge-redis` | `redis:7-alpine` | Up 6 hours, healthy | 1.52% | 7.742 MiB / 15.4 GiB | 9 | internal Redis port `6379` |
| `postforge-db` | `pgvector/pgvector:0.8.2-pg18-trixie` | Up 6 hours, healthy | 0.00% | 43.52 MiB / 15.4 GiB | 19 | internal PostgreSQL port `5432` |

해석: 이 snapshot은 유휴 또는 저트래픽 상태에 가깝다. 수집 시점에 명확한 메모리 압박이 없다는 근거는 되지만, 운영 부하 수용량을 증명하지는 않는다.

## 성능 해석 메모

- 수집 시점 prod는 Intel N100 host, 4 logical CPUs, 15 GiB memory 환경이다.
- 이 디렉터리의 이전 리포트 일부는 Oracle Cloud ARM 1 vCPU / 1 GB 환경을 기준으로 한다. 해당 리포트는 병목 분석의 historical baseline으로는 유효하지만, 현재 host의 RPS 상한으로 재사용하면 안 된다.
- 수집된 Docker stats 기준 container에는 별도 memory limit이 걸려 있지 않다. 각 container가 host-level `15.4 GiB` limit을 보고 있다. 예측 가능한 메모리 격리가 필요해지면 container memory limit과 JVM heap sizing을 명시한다.
- 수집 시점 app container memory 사용량은 약 586 MiB다. 실제 부하 테스트에서는 이 값을 Prometheus의 `jvm_memory_used_bytes`, GC, request load와 같은 시간 구간으로 비교해야 한다.
- root disk 사용률은 21%였으므로, 수집 시점에는 disk capacity가 즉시 병목이 아니다.

## 다음에 수집할 지표

부하 테스트와 같은 시간 구간에서 아래 Prometheus 지표를 함께 수집한다.

```promql
process_cpu_usage
system_cpu_usage
jvm_memory_used_bytes
jvm_memory_committed_bytes
jvm_gc_pause_seconds_count
jvm_gc_pause_seconds_sum
hikaricp_connections_active
hikaricp_connections_pending
http_server_requests_seconds_count
http_server_requests_seconds_sum
external_naver_fetch_seconds_count
external_source_db_persist_seconds_count
ai_text_generation_seconds_count
ai_text_generation_completion_tokens_sum
```

진지한 baseline run 전에는 PostgreSQL, Redis 설정 snapshot도 함께 수집한다.

```sql
select version();
show max_connections;
show shared_buffers;
show work_mem;
show effective_cache_size;
select count(*) as active_connections from pg_stat_activity;
```

```bash
redis-cli INFO server
redis-cli INFO memory
redis-cli INFO stats
```
