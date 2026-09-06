# MySQL 파티셔닝 재검증

`spring-data/mysql-partition.md` 의 1차 측정에서 발견한 결함 6건을 제거하고 다시 측정한 실험이다.
설계 근거와 결함 목록은 [PLAN.md](./PLAN.md) 에 있다.

## 구성

| 파일 | 역할 |
| --- | --- |
| `PLAN.md` | 1차 측정의 결함과 이번 실험 설계 |
| `01-schema.sql` | 상품 3종(비파티션 PK(id) / 비파티션 복합 PK / 파티션) + 댓글 스키마 |
| `gen-seed.sh` | 데이터셋별 시드 SQL 생성 (`uniform` / `skewed`) |
| `bench.js` | k6 부하 스크립트 (엔드포인트는 환경변수로 주입) |
| `run-bench.sh` | 재기동 → 워밍업 → 3회 측정 프로토콜 실행기 |
| `capture-explain.sh` | 쿼리 플랜·데이터 불변식 기록 |
| `summarize.py` | k6 결과를 비교표로 집계 |
| `results/<데이터셋>/` | k6 원본 요약 JSON·로그, `explain.md` |
| `ablation-schema.sql` · `gen-ablation-seed.sh` | 날짜 인덱스 대조 실험 스키마·시드 (상품 3벌, 댓글 미사용) |
| `ablation-lib.sh` · `run-ablation.sh` | 대조 실험 실행기. 컨테이너 소실 자가 복구와 오염 반복 재측정을 포함한다 |
| `check-rep.py` · `check-case.py` | 실패율 판정 — 반복 재측정과 완료 케이스 건너뛰기에 쓴다 |

## 실행 순서

```bash
# 1. MySQL 기동 (포트 3316)
docker run -d --name mysql-partition-rebench -e MYSQL_ROOT_PASSWORD=root -p 3316:3306 mysql:8.0 \
  --innodb-buffer-pool-size=2G --innodb-log-file-size=512M \
  --innodb-flush-log-at-trx-commit=2 --innodb-flush-method=O_DIRECT \
  --log-bin-trust-function-creators=1

# 2. 데이터셋 시드 (상품 1,000만 / 댓글 2,000만, 약 20분)
docker exec -i mysql-partition-rebench mysql -uroot -proot -e "CREATE DATABASE partition_uniform"
docker exec -i mysql-partition-rebench mysql -uroot -proot partition_uniform < 01-schema.sql
./gen-seed.sh uniform 10000000 20000000 | docker exec -i mysql-partition-rebench mysql -uroot -proot partition_uniform

# 3. 앱 기동
cd ../.. && ./gradlew bootJar -x test
REBENCH_DB=partition_uniform java -jar build/libs/spring-data-0.0.1-SNAPSHOT.jar --spring.profiles.active=rebench

# 4. 플랜 기록 → 측정 → 집계
./capture-explain.sh uniform
./run-bench.sh uniform
./summarize.py uniform
```

`skewed` 데이터셋도 같은 순서로 반복한다. 두 데이터셋을 한 인스턴스에 함께 두면 버퍼 풀을 나눠 쓰게 되므로,
한 데이터셋 측정을 끝내고 `DROP DATABASE` 한 뒤 다음 데이터셋을 시드한다.

## 읽는 법

- 표의 값은 3회 반복의 **중앙값**이고 괄호는 최소~최대다.
- 반복 간 변동폭이 두 비교군의 차이보다 크면 "차이 없음" 으로 읽는다.
- 부하는 50 VU 클로즈드 모델에 think time 이 없다. TPS 와 레이턴시는 독립된 지표가 아니라
  같은 사실의 두 표현이므로, 둘을 각각 개선 근거로 세지 않는다.
