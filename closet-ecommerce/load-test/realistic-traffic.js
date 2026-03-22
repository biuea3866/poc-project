import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ============================================================
// Closet E-Commerce 실시간 트래픽 시뮬레이터
// 실제 유저 행동 패턴을 시뮬레이션합니다.
//
// 사용법:
//   k6 run load-test/realistic-traffic.js                    # 기본 (10 VU, 5분)
//   k6 run --vus 50 --duration 30m load-test/realistic-traffic.js  # 50명, 30분
//   k6 run --vus 100 --duration 1h load-test/realistic-traffic.js  # 100명, 1시간 (지속)
// ============================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// 커스텀 메트릭
const orderCreated = new Counter('orders_created');
const cartAdded = new Counter('cart_items_added');
const signupCount = new Counter('signups');
const loginCount = new Counter('logins');
const paymentCount = new Counter('payments_completed');
const browseCount = new Counter('products_browsed');
const errorRate = new Rate('error_rate');
const orderDuration = new Trend('order_flow_duration');

// 설정
export const options = {
  scenarios: {
    // 1. 구경만 하는 유저 (전체의 60%)
    browsers: {
      executor: 'constant-vus',
      vus: __ENV.BROWSERS || 6,
      duration: __ENV.DURATION || '5m',
      exec: 'browsingUser',
    },
    // 2. 장바구니까지 가는 유저 (전체의 25%)
    shoppers: {
      executor: 'constant-vus',
      vus: __ENV.SHOPPERS || 3,
      duration: __ENV.DURATION || '5m',
      exec: 'shoppingUser',
    },
    // 3. 실제 구매하는 유저 (전체의 15%)
    buyers: {
      executor: 'constant-vus',
      vus: __ENV.BUYERS || 1,
      duration: __ENV.DURATION || '5m',
      exec: 'buyingUser',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // p95 < 2초
    error_rate: ['rate<0.1'],            // 에러율 10% 미만
  },
};

const headers = { 'Content-Type': 'application/json' };

// ============================================================
// 유틸리티 함수
// ============================================================

function registerAndLogin() {
  const uniqueId = `${Date.now()}-${randomIntBetween(1000, 9999)}`;
  const email = `user-${uniqueId}@loadtest.com`;

  // 회원가입
  const registerRes = http.post(`${BASE_URL}/members/register`, JSON.stringify({
    email: email,
    password: 'password123',
    name: `테스터${uniqueId}`,
    phone: `010${randomIntBetween(10000000, 99999999)}`,
  }), { headers });

  if (registerRes.status !== 200 && registerRes.status !== 201) {
    errorRate.add(1);
    return null;
  }
  signupCount.add(1);

  // 로그인
  const loginRes = http.post(`${BASE_URL}/members/login`, JSON.stringify({
    email: email,
    password: 'password123',
  }), { headers });

  if (loginRes.status !== 200) {
    errorRate.add(1);
    return null;
  }
  loginCount.add(1);

  const body = JSON.parse(loginRes.body);
  return {
    token: body.data.accessToken,
    memberId: body.data.memberId,
    email: email,
  };
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Member-Id': '1', // 기본값
  };
}

function browseProducts(page, size) {
  const res = http.get(`${BASE_URL}/products?page=${page || 0}&size=${size || 10}`);
  check(res, { '상품 목록 조회 성공': (r) => r.status === 200 });
  browseCount.add(1);
  errorRate.add(res.status !== 200);

  if (res.status === 200) {
    const body = JSON.parse(res.body);
    return body.data?.content || [];
  }
  return [];
}

function viewProductDetail(productId) {
  const res = http.get(`${BASE_URL}/products/${productId}`);
  check(res, { '상품 상세 조회 성공': (r) => r.status === 200 });
  browseCount.add(1);
  errorRate.add(res.status !== 200);

  if (res.status === 200) {
    return JSON.parse(res.body).data;
  }
  return null;
}

function browseCategories() {
  const res = http.get(`${BASE_URL}/categories`);
  check(res, { '카테고리 조회 성공': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);
}

function browseBrands() {
  const res = http.get(`${BASE_URL}/brands`);
  check(res, { '브랜드 조회 성공': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);
}

function filterByCategory(categoryId) {
  const res = http.get(`${BASE_URL}/products?categoryId=${categoryId}&page=0&size=10`);
  check(res, { '카테고리 필터 성공': (r) => r.status === 200 });
  browseCount.add(1);
  errorRate.add(res.status !== 200);
}

// ============================================================
// 시나리오 1: 구경만 하는 유저 (Browser)
// 메인 → 카테고리 탐색 → 상품 목록 → 상품 상세 2~3개 → 이탈
// ============================================================

export function browsingUser() {
  group('브라우징 유저', () => {
    // 1. 메인 페이지 (상품 목록)
    browseProducts(0, 8);
    sleep(randomIntBetween(2, 5));

    // 2. 카테고리 탐색
    browseCategories();
    sleep(randomIntBetween(1, 3));

    // 3. 카테고리별 필터
    const categoryId = randomIntBetween(1, 6);
    filterByCategory(categoryId);
    sleep(randomIntBetween(2, 4));

    // 4. 브랜드 탐색
    browseBrands();
    sleep(randomIntBetween(1, 2));

    // 5. 상품 2~3개 상세 보기
    const products = browseProducts(randomIntBetween(0, 2), 10);
    const viewCount = randomIntBetween(2, 4);
    for (let i = 0; i < Math.min(viewCount, products.length); i++) {
      viewProductDetail(products[i].id);
      sleep(randomIntBetween(3, 8)); // 상품 상세 읽는 시간
    }

    // 6. 다른 페이지 탐색
    browseProducts(randomIntBetween(0, 3), 10);
    sleep(randomIntBetween(2, 5));
  });
}

// ============================================================
// 시나리오 2: 장바구니까지 가는 유저 (Shopper)
// 회원가입 → 로그인 → 상품 탐색 → 장바구니 담기 2~3개 → 이탈
// ============================================================

export function shoppingUser() {
  group('쇼핑 유저', () => {
    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) {
      sleep(5);
      return;
    }
    sleep(randomIntBetween(1, 3));

    // 2. 상품 탐색
    const products = browseProducts(0, 20);
    sleep(randomIntBetween(2, 4));

    // 3. 상품 상세 보기 + 장바구니 담기
    const itemsToAdd = randomIntBetween(1, 3);
    for (let i = 0; i < Math.min(itemsToAdd, products.length); i++) {
      const product = products[randomIntBetween(0, products.length - 1)];

      // 상세 보기
      const detail = viewProductDetail(product.id);
      sleep(randomIntBetween(3, 6));

      if (detail && detail.options && detail.options.length > 0) {
        // 장바구니 담기
        const option = detail.options[randomIntBetween(0, detail.options.length - 1)];
        const addRes = http.post(`${BASE_URL}/carts/items`, JSON.stringify({
          productId: product.id,
          productOptionId: option.id,
          quantity: randomIntBetween(1, 3),
          unitPrice: product.salePrice || product.basePrice,
        }), {
          headers: {
            ...headers,
            'X-Member-Id': `${auth.memberId}`,
          },
        });

        check(addRes, { '장바구니 담기 성공': (r) => r.status === 200 || r.status === 201 });
        cartAdded.add(1);
        errorRate.add(addRes.status !== 200 && addRes.status !== 201);
        sleep(randomIntBetween(1, 3));
      }
    }

    // 4. 장바구니 확인
    const cartRes = http.get(`${BASE_URL}/carts`, {
      headers: { 'X-Member-Id': `${auth.memberId}` },
    });
    check(cartRes, { '장바구니 조회 성공': (r) => r.status === 200 });
    sleep(randomIntBetween(2, 5));

    // 5. 이탈 (구매 안 함)
  });
}

// ============================================================
// 시나리오 3: 실제 구매하는 유저 (Buyer)
// 회원가입 → 로그인 → 배송지 등록 → 상품 탐색 → 장바구니 → 주문 → 결제
// ============================================================

export function buyingUser() {
  group('구매 유저', () => {
    const startTime = Date.now();

    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) {
      sleep(5);
      return;
    }
    sleep(randomIntBetween(1, 2));

    // 2. 배송지 등록
    const addrRes = http.post(`${BASE_URL}/members/me/addresses`, JSON.stringify({
      name: '테스터',
      phone: `010${randomIntBetween(10000000, 99999999)}`,
      zipCode: '06035',
      address: '서울특별시 강남구 테헤란로',
      detailAddress: `${randomIntBetween(1, 30)}층 ${randomIntBetween(100, 999)}호`,
    }), { headers: authHeaders(auth.token) });
    check(addrRes, { '배송지 등록 성공': (r) => r.status === 200 || r.status === 201 });
    sleep(randomIntBetween(1, 2));

    // 3. 상품 탐색
    const products = browseProducts(0, 20);
    sleep(randomIntBetween(2, 4));

    if (products.length === 0) {
      sleep(5);
      return;
    }

    // 4. 상품 선택 + 상세 보기
    const selectedProduct = products[randomIntBetween(0, products.length - 1)];
    const detail = viewProductDetail(selectedProduct.id);
    sleep(randomIntBetween(3, 6));

    if (!detail || !detail.options || detail.options.length === 0) {
      sleep(5);
      return;
    }

    const selectedOption = detail.options[randomIntBetween(0, detail.options.length - 1)];
    const quantity = randomIntBetween(1, 2);
    const unitPrice = selectedProduct.salePrice || selectedProduct.basePrice;

    // 5. 장바구니 담기
    const addCartRes = http.post(`${BASE_URL}/carts/items`, JSON.stringify({
      productId: selectedProduct.id,
      productOptionId: selectedOption.id,
      quantity: quantity,
      unitPrice: unitPrice,
    }), {
      headers: { ...headers, 'X-Member-Id': `${auth.memberId}` },
    });
    check(addCartRes, { '장바구니 담기 성공': (r) => r.status === 200 || r.status === 201 });
    cartAdded.add(1);
    sleep(randomIntBetween(1, 3));

    // 6. 주문 생성
    const orderRes = http.post(`${BASE_URL}/orders`, JSON.stringify({
      memberId: auth.memberId,
      sellerId: 1,
      items: [{
        productId: selectedProduct.id,
        productOptionId: selectedOption.id,
        productName: selectedProduct.name,
        optionName: `${selectedOption.size}/${selectedOption.colorName}`,
        quantity: quantity,
        unitPrice: unitPrice,
      }],
      receiverName: '테스터',
      receiverPhone: '01012345678',
      zipCode: '06035',
      address: '서울특별시 강남구',
      detailAddress: '테스트빌딩 1층',
      shippingFee: unitPrice * quantity >= 50000 ? 0 : 3000,
    }), {
      headers: { ...headers, 'X-Member-Id': `${auth.memberId}` },
    });

    const orderSuccess = check(orderRes, { '주문 생성 성공': (r) => r.status === 200 || r.status === 201 });
    errorRate.add(!orderSuccess);
    sleep(randomIntBetween(1, 3));

    if (orderSuccess) {
      const orderData = JSON.parse(orderRes.body);
      const orderId = orderData.data?.id;
      orderCreated.add(1);

      if (orderId) {
        // 7. 결제 승인
        const paymentKey = `k6-pay-${Date.now()}-${randomIntBetween(1000, 9999)}`;
        const payRes = http.post(`${BASE_URL}/payments/confirm`, JSON.stringify({
          paymentKey: paymentKey,
          orderId: orderId,
          amount: unitPrice * quantity,
        }), { headers });

        const paySuccess = check(payRes, { '결제 승인 성공': (r) => r.status === 200 });
        if (paySuccess) {
          paymentCount.add(1);
        }
        errorRate.add(!paySuccess);

        // 8. 주문 확인
        sleep(randomIntBetween(1, 2));
        const orderCheckRes = http.get(`${BASE_URL}/orders/${orderId}`, {
          headers: { 'X-Member-Id': `${auth.memberId}` },
        });
        check(orderCheckRes, { '주문 조회 성공': (r) => r.status === 200 });
      }
    }

    const duration = Date.now() - startTime;
    orderDuration.add(duration);
    sleep(randomIntBetween(3, 8));
  });
}
