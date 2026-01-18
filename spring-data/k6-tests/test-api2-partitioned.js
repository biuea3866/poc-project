import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    errors: ['rate<0.1'],
  },
};

export default function () {
  const url = 'http://localhost:8080/api/partition-test/products/partitioned?page=0&size=100&startDate=2024-01-01&endDate=2024-12-31';

  const res = http.get(url);

  const result = check(res, {
    'status is 200': (r) => r.status === 200,
    'response has content': (r) => r.json().content !== undefined,
  });

  errorRate.add(!result);

  sleep(0.1);
}
