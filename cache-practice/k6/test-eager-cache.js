import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export const errorRate = new Rate('errors');

export const options = {
  duration: '2m',
  vus: 100,
};

export default function () {
  const orderId = Math.floor(Math.random() * 1000000) + 1;
  const res = http.get(`http://localhost:8080/api/orders/eager/${orderId}`);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  errorRate.add(!success);
  sleep(0.1);
}
