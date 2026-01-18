import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 30 },  // Ramp up to 30 users (lower for JOIN queries)
    { duration: '1m', target: 30 },   // Stay at 30 users
    { duration: '30s', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95% of requests should complete within 3s (more lenient for JOIN)
    errors: ['rate<0.1'],              // Error rate should be less than 10%
  },
};

export default function () {
  const url = 'http://localhost:8080/api/partition-test/products-with-comments/without-partition-key?page=0&size=10&productStartDate=2024-01-01&productEndDate=2024-12-31';

  const res = http.get(url);

  const result = check(res, {
    'status is 200': (r) => r.status === 200,
    'response is array': (r) => Array.isArray(r.json()),
  });

  errorRate.add(!result);

  sleep(0.2);
}
