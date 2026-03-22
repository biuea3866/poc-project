import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ============================================================
// Closet E-Commerce — 스파이크 트래픽 테스트
//
// 타임세일이나 이벤트 시 급격한 트래픽 증가를 시뮬레이션합니다.
//
// 사용법:
//   k6 run load-test/spike-traffic.js
// ============================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// 커스텀 메트릭
const pageViews = new Counter('page_views');
const searchCount = new Counter('searches');
const orderCompleted = new Counter('orders_completed');
const revenueTotal = new Counter('revenue_total');
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');

export const options = {
  stages: [
    { duration: '2m', target: 20 },   // 워밍업
    { duration: '5m', target: 50 },   // 정상 트래픽
    { duration: '1m', target: 200 },  // 타임세일 스파이크!
    { duration: '3m', target: 200 },  // 스파이크 유지
    { duration: '2m', target: 50 },   // 정상 복귀
    { duration: '2m', target: 0 },    // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],   // 스파이크 시 p95 < 5초
    error_rate: ['rate<0.3'],             // 스파이크 허용 에러율 30%
  },
};

const defaultHeaders = { 'Content-Type': 'application/json' };

const SEARCH_KEYWORDS = [
  '반팔', '청바지', '후드', '패딩', '운동화', '맨투맨',
  '슬랙스', '자켓', '코트', '니트', '셔츠', '원피스',
  '타임세일', '한정판', '신상',
];

// ============================================================
// 유틸리티 함수
// ============================================================

function safeRequest(fn) {
  try {
    const start = Date.now();
    const res = fn();
    responseTime.add(Date.now() - start);
    return res;
  } catch (e) {
    errorRate.add(1);
    return null;
  }
}

function browseProducts(page, size) {
  const res = safeRequest(() =>
    http.get(`${BASE_URL}/products?page=${page || 0}&size=${size || 10}`)
  );
  if (!res) return [];

  const ok = check(res, { '상품 목록 조회': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data?.content || []; }
    catch (e) { return []; }
  }
  return [];
}

function viewProductDetail(productId) {
  const res = safeRequest(() => http.get(`${BASE_URL}/products/${productId}`));
  if (!res) return null;

  const ok = check(res, { '상품 상세 조회': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data; }
    catch (e) { return null; }
  }
  return null;
}

function searchProducts(keyword) {
  const res = safeRequest(() =>
    http.get(`${BASE_URL}/products?keyword=${encodeURIComponent(keyword)}&page=0&size=20`)
  );
  if (!res) return [];

  const ok = check(res, { '상품 검색': (r) => r.status === 200 });
  searchCount.add(1);
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data?.content || []; }
    catch (e) { return []; }
  }
  return [];
}

function registerAndLogin() {
  const uniqueId = `${Date.now()}-${randomIntBetween(1000, 9999)}`;
  const email = `spike-${uniqueId}@loadtest.com`;

  const registerRes = safeRequest(() =>
    http.post(`${BASE_URL}/members/register`, JSON.stringify({
      email: email,
      password: 'password123',
      name: `스파이크테스터${uniqueId}`,
      phone: `010${randomIntBetween(10000000, 99999999)}`,
    }), { headers: defaultHeaders })
  );

  if (!registerRes || (registerRes.status !== 200 && registerRes.status !== 201)) {
    errorRate.add(1);
    return null;
  }

  const loginRes = safeRequest(() =>
    http.post(`${BASE_URL}/members/login`, JSON.stringify({
      email: email,
      password: 'password123',
    }), { headers: defaultHeaders })
  );

  if (!loginRes || loginRes.status !== 200) {
    errorRate.add(1);
    return null;
  }

  try {
    const body = JSON.parse(loginRes.body);
    return {
      token: body.data.accessToken,
      memberId: body.data.memberId,
      email: email,
    };
  } catch (e) {
    errorRate.add(1);
    return null;
  }
}

// ============================================================
// 기본 시나리오 — VU마다 랜덤 행동
// ============================================================

export default function () {
  const behavior = Math.random();

  if (behavior < 0.5) {
    // 50%: 단순 브라우징 (타임세일 구경)
    group('타임세일 구경', () => {
      const products = browseProducts(0, 20);
      sleep(randomIntBetween(1, 3));

      // 검색
      searchProducts(randomItem(SEARCH_KEYWORDS));
      sleep(randomIntBetween(1, 3));

      // 상품 1~3개 상세
      const viewCnt = randomIntBetween(1, 3);
      for (let i = 0; i < Math.min(viewCnt, products.length); i++) {
        viewProductDetail(products[i].id);
        sleep(randomIntBetween(1, 4));
      }
    });

  } else if (behavior < 0.8) {
    // 30%: 빠른 구매 시도 (타임세일 잡으려고)
    group('타임세일 구매 시도', () => {
      const auth = registerAndLogin();
      if (!auth) { sleep(2); return; }

      // 배송지 등록
      safeRequest(() =>
        http.post(`${BASE_URL}/members/me/addresses`, JSON.stringify({
          name: '테스터',
          phone: `010${randomIntBetween(10000000, 99999999)}`,
          zipCode: '06035',
          address: '서울특별시 강남구 테헤란로',
          detailAddress: `${randomIntBetween(1, 20)}층`,
        }), {
          headers: {
            ...defaultHeaders,
            'Authorization': `Bearer ${auth.token}`,
          },
        })
      );
      sleep(randomIntBetween(1, 2));

      // 상품 목록
      const products = browseProducts(0, 10);
      if (products.length === 0) { sleep(2); return; }
      sleep(randomIntBetween(1, 2));

      // 상세 보기
      const product = products[randomIntBetween(0, products.length - 1)];
      const detail = viewProductDetail(product.id);
      if (!detail || !detail.options || detail.options.length === 0) { sleep(2); return; }
      sleep(randomIntBetween(1, 2));

      const option = detail.options[randomIntBetween(0, detail.options.length - 1)];
      const unitPrice = product.salePrice || product.basePrice || 29900;
      const memberHeaders = { 'Content-Type': 'application/json', 'X-Member-Id': `${auth.memberId}` };

      // 장바구니 담기
      safeRequest(() =>
        http.post(`${BASE_URL}/carts/items`, JSON.stringify({
          productId: product.id,
          productOptionId: option.id,
          quantity: 1,
          unitPrice: unitPrice,
        }), { headers: memberHeaders })
      );
      sleep(randomIntBetween(1, 2));

      // 주문
      const orderRes = safeRequest(() =>
        http.post(`${BASE_URL}/orders`, JSON.stringify({
          memberId: auth.memberId,
          sellerId: 1,
          items: [{
            productId: product.id,
            productOptionId: option.id,
            productName: product.name,
            optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
            quantity: 1,
            unitPrice: unitPrice,
          }],
          receiverName: '테스터',
          receiverPhone: '01012345678',
          zipCode: '06035',
          address: '서울특별시 강남구',
          detailAddress: '테스트빌딩',
          shippingFee: unitPrice >= 50000 ? 0 : 3000,
        }), { headers: memberHeaders })
      );

      if (orderRes && (orderRes.status === 200 || orderRes.status === 201)) {
        try {
          const orderData = JSON.parse(orderRes.body).data;
          if (orderData && orderData.id) {
            orderCompleted.add(1);
            revenueTotal.add(unitPrice);

            // 결제
            safeRequest(() =>
              http.post(`${BASE_URL}/payments/confirm`, JSON.stringify({
                paymentKey: `spike-pay-${Date.now()}-${randomIntBetween(1000, 9999)}`,
                orderId: orderData.id,
                amount: unitPrice,
              }), { headers: defaultHeaders })
            );
          }
        } catch (e) { /* ignore */ }
      }
      sleep(randomIntBetween(1, 3));
    });

  } else {
    // 20%: 검색 폭주 (타임세일 상품 찾기)
    group('타임세일 검색 폭주', () => {
      const searchesToDo = randomIntBetween(3, 6);
      for (let i = 0; i < searchesToDo; i++) {
        searchProducts(randomItem(SEARCH_KEYWORDS));
        sleep(randomIntBetween(1, 3));
      }

      // 상품 목록도 여러 페이지
      for (let page = 0; page < randomIntBetween(2, 4); page++) {
        browseProducts(page, 20);
        sleep(randomIntBetween(1, 2));
      }
    });
  }
}
