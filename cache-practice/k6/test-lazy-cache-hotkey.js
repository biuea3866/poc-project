import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
};

// Zipf distribution 시뮬레이션: 상위 10% 데이터에 80% 접근
function getHotKeyOrderId() {
  const rand = Math.random();

  if (rand < 0.8) {
    // 80% 확률로 상위 10,000개 중 선택 (hot keys)
    return Math.floor(Math.random() * 10000) + 1;
  } else {
    // 20% 확률로 나머지 990,000개 중 선택 (cold keys)
    return Math.floor(Math.random() * 990000) + 10001;
  }
}

export default function () {
  const orderId = getHotKeyOrderId();
  const res = http.get(`http://localhost:8080/api/orders/lazy/${orderId}`);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  errorRate.add(!success);
  sleep(0.1);
}
