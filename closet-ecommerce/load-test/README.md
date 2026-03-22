# Closet 부하 테스트 (k6)

## 시나리오

실제 이커머스 유저 행동 패턴을 시뮬레이션합니다.

| 유저 타입 | 비율 | 행동 |
|----------|------|------|
| **브라우저** | 60% | 상품 탐색 → 카테고리 필터 → 상세 2~3개 → 이탈 |
| **쇼퍼** | 25% | 회원가입 → 로그인 → 탐색 → 장바구니 담기 → 이탈 |
| **구매자** | 15% | 회원가입 → 배송지 → 탐색 → 장바구니 → 주문 → 결제 |

## 실행

```bash
# 기본 (10 VU, 5분)
k6 run load-test/realistic-traffic.js

# 50명 유저, 30분 지속
k6 run --duration 30m \
  -e BROWSERS=30 -e SHOPPERS=13 -e BUYERS=7 \
  load-test/realistic-traffic.js

# 100명 유저, 1시간 지속 (타임세일 시뮬레이션)
k6 run --duration 1h \
  -e BROWSERS=60 -e SHOPPERS=25 -e BUYERS=15 \
  load-test/realistic-traffic.js

# 백그라운드 실행 (지속적 트래픽)
nohup k6 run --duration 24h \
  -e BROWSERS=5 -e SHOPPERS=3 -e BUYERS=2 \
  load-test/realistic-traffic.js > /tmp/closet-logs/k6.log 2>&1 &
```

## 커스텀 메트릭

| 메트릭 | 설명 |
|--------|------|
| `orders_created` | 생성된 주문 수 |
| `cart_items_added` | 장바구니 담기 횟수 |
| `signups` | 회원가입 수 |
| `logins` | 로그인 수 |
| `payments_completed` | 결제 완료 수 |
| `products_browsed` | 상품 조회 수 |
| `error_rate` | 에러 발생률 |
| `order_flow_duration` | 주문 전체 플로우 소요 시간 |

## Grafana 연동

k6 결과를 Grafana에서 보려면:
```bash
k6 run --out influxdb=http://localhost:8086/k6 load-test/realistic-traffic.js
```

## Makefile

```bash
make load-light    # 10 VU, 5분
make load-medium   # 50 VU, 30분
make load-heavy    # 100 VU, 1시간
make load-bg       # 백그라운드 24시간
```
