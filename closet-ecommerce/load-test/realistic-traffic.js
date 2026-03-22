import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ============================================================
// Closet E-Commerce — 8가지 유저 행동 패턴 트래픽 시뮬레이터
//
// 시나리오:
//   1. 윈도우 쇼퍼 (30%) — 구경만 하고 이탈
//   2. 비교 쇼퍼 (15%) — 꼼꼼히 비교, 결국 안 삼
//   3. 충동 구매자 (10%) — 빠르게 보고 바로 구매
//   4. 장바구니 모아두기 (15%) — 담고 이탈
//   5. 신규 회원 (10%) — 첫 방문, 탐색 위주
//   6. 단골 구매자 (10%) — 로그인 후 바로 구매
//   7. 반복 방문자 (5%) — 여러 세션, 마지막에 구매
//   8. 대량 구매자 (5%) — 많이 담고 한번에 주문
//
// 사용법:
//   k6 run load-test/realistic-traffic.js                        # 기본 (50 VU, 10분)
//   k6 run -e DURATION=1h load-test/realistic-traffic.js         # 50 VU, 1시간
// ============================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// ============================================================
// 커스텀 메트릭
// ============================================================
const pageViews = new Counter('page_views');
const searchCount = new Counter('searches');
const cartAbandoned = new Counter('cart_abandoned');
const orderCompleted = new Counter('orders_completed');
const revenueTotal = new Counter('revenue_total');
const avgSessionDuration = new Trend('avg_session_duration');
const conversionFunnel = new Trend('conversion_funnel_step');

const signupCount = new Counter('signups');
const loginCount = new Counter('logins');
const errorRate = new Rate('error_rate');
const orderDuration = new Trend('order_flow_duration');

// ============================================================
// 시나리오 설정 — 50 VU 총합
// ============================================================
export const options = {
  scenarios: {
    window_shoppers: {
      executor: 'constant-vus',
      vus: 15,
      duration: __ENV.DURATION || '10m',
      exec: 'windowShopper',
    },
    comparison_shoppers: {
      executor: 'constant-vus',
      vus: 8,
      duration: __ENV.DURATION || '10m',
      exec: 'comparisonShopper',
    },
    impulse_buyers: {
      executor: 'constant-vus',
      vus: 5,
      duration: __ENV.DURATION || '10m',
      exec: 'impulseBuyer',
    },
    cart_abandoners: {
      executor: 'constant-vus',
      vus: 8,
      duration: __ENV.DURATION || '10m',
      exec: 'cartAbandoner',
    },
    new_members: {
      executor: 'constant-vus',
      vus: 5,
      duration: __ENV.DURATION || '10m',
      exec: 'newMember',
    },
    loyal_buyers: {
      executor: 'constant-vus',
      vus: 5,
      duration: __ENV.DURATION || '10m',
      exec: 'loyalBuyer',
    },
    repeat_visitors: {
      executor: 'constant-vus',
      vus: 2,
      duration: __ENV.DURATION || '10m',
      exec: 'repeatVisitor',
    },
    bulk_buyers: {
      executor: 'constant-vus',
      vus: 2,
      duration: __ENV.DURATION || '10m',
      exec: 'bulkBuyer',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // p95 < 3초
    error_rate: ['rate<0.15'],           // 에러율 15% 미만
  },
};

const defaultHeaders = { 'Content-Type': 'application/json' };

const CATEGORIES = [1, 2, 3, 4, 5, 6]; // 상의, 하의, 아우터, 신발, 액세서리, 기타
const SORT_OPTIONS = ['createdAt,desc', 'salePrice,asc', 'salePrice,desc', 'name,asc'];
const SEARCH_KEYWORDS = [
  '반팔', '청바지', '후드', '패딩', '운동화', '맨투맨',
  '슬랙스', '자켓', '코트', '니트', '셔츠', '원피스',
  '가디건', '트레이닝', '조거팬츠', '스니커즈',
];
const BRAND_IDS = [1, 2, 3, 4, 5];
const PRICE_RANGES = [
  { min: 0, max: 30000 },
  { min: 30000, max: 50000 },
  { min: 50000, max: 100000 },
  { min: 100000, max: 300000 },
];

// ============================================================
// 유틸리티 함수
// ============================================================

function safeRequest(fn) {
  try {
    return fn();
  } catch (e) {
    errorRate.add(1);
    return null;
  }
}

function registerAndLogin() {
  const uniqueId = `${Date.now()}-${randomIntBetween(1000, 9999)}`;
  const email = `user-${uniqueId}@loadtest.com`;

  const registerRes = safeRequest(() =>
    http.post(`${BASE_URL}/members/register`, JSON.stringify({
      email: email,
      password: 'password123',
      name: `테스터${uniqueId}`,
      phone: `010${randomIntBetween(10000000, 99999999)}`,
    }), { headers: defaultHeaders })
  );

  if (!registerRes || (registerRes.status !== 200 && registerRes.status !== 201)) {
    errorRate.add(1);
    return null;
  }
  signupCount.add(1);
  conversionFunnel.add(1); // step 1: 회원가입

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
  loginCount.add(1);
  conversionFunnel.add(2); // step 2: 로그인

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

function loginOnly(email, password) {
  const loginRes = safeRequest(() =>
    http.post(`${BASE_URL}/members/login`, JSON.stringify({
      email: email,
      password: password,
    }), { headers: defaultHeaders })
  );

  if (!loginRes || loginRes.status !== 200) {
    errorRate.add(1);
    return null;
  }
  loginCount.add(1);

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

function memberHeaders(memberId) {
  return {
    'Content-Type': 'application/json',
    'X-Member-Id': `${memberId}`,
  };
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

function browseProducts(page, size, extraParams) {
  let url = `${BASE_URL}/products?page=${page || 0}&size=${size || 10}`;
  if (extraParams) url += `&${extraParams}`;

  const res = safeRequest(() => http.get(url));
  if (!res) return [];

  const ok = check(res, { '상품 목록 조회 성공': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try {
      const body = JSON.parse(res.body);
      return body.data?.content || [];
    } catch (e) { return []; }
  }
  return [];
}

function viewProductDetail(productId) {
  const res = safeRequest(() => http.get(`${BASE_URL}/products/${productId}`));
  if (!res) return null;

  const ok = check(res, { '상품 상세 조회 성공': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data; }
    catch (e) { return null; }
  }
  return null;
}

function browseCategories() {
  const res = safeRequest(() => http.get(`${BASE_URL}/categories`));
  if (!res) return [];

  const ok = check(res, { '카테고리 조회 성공': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data || []; }
    catch (e) { return []; }
  }
  return [];
}

function browseBrands() {
  const res = safeRequest(() => http.get(`${BASE_URL}/brands`));
  if (!res) return [];

  const ok = check(res, { '브랜드 조회 성공': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data || []; }
    catch (e) { return []; }
  }
  return [];
}

function searchProducts(keyword) {
  const res = safeRequest(() =>
    http.get(`${BASE_URL}/products?keyword=${encodeURIComponent(keyword)}&page=0&size=20`)
  );
  if (!res) return [];

  const ok = check(res, { '상품 검색 성공': (r) => r.status === 200 });
  searchCount.add(1);
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data?.content || []; }
    catch (e) { return []; }
  }
  return [];
}

function filterProducts(categoryId, brandId, priceRange, sort) {
  let params = `page=0&size=20`;
  if (categoryId) params += `&categoryId=${categoryId}`;
  if (brandId) params += `&brandId=${brandId}`;
  if (priceRange) params += `&minPrice=${priceRange.min}&maxPrice=${priceRange.max}`;
  if (sort) params += `&sort=${sort}`;

  return browseProducts(0, 20, params.replace('page=0&size=20&', ''));
}

function addToCart(memberId, productId, optionId, quantity, unitPrice) {
  const res = safeRequest(() =>
    http.post(`${BASE_URL}/carts/items`, JSON.stringify({
      productId: productId,
      productOptionId: optionId,
      quantity: quantity,
      unitPrice: unitPrice,
    }), { headers: memberHeaders(memberId) })
  );

  if (!res) return false;

  const ok = check(res, { '장바구니 담기 성공': (r) => r.status === 200 || r.status === 201 });
  errorRate.add(!ok);
  conversionFunnel.add(3); // step 3: 장바구니
  return ok;
}

function viewCart(memberId) {
  const res = safeRequest(() =>
    http.get(`${BASE_URL}/carts`, { headers: memberHeaders(memberId) })
  );
  if (!res) return null;

  const ok = check(res, { '장바구니 조회 성공': (r) => r.status === 200 });
  pageViews.add(1);
  errorRate.add(!ok);

  if (ok) {
    try { return JSON.parse(res.body).data; }
    catch (e) { return null; }
  }
  return null;
}

function registerAddress(token, name) {
  const res = safeRequest(() =>
    http.post(`${BASE_URL}/members/me/addresses`, JSON.stringify({
      name: name || '테스터',
      phone: `010${randomIntBetween(10000000, 99999999)}`,
      zipCode: randomItem(['06035', '04524', '07281', '13494', '34014']),
      address: randomItem([
        '서울특별시 강남구 테헤란로',
        '서울특별시 마포구 월드컵북로',
        '서울특별시 송파구 올림픽로',
        '경기도 성남시 분당구 판교로',
        '대전광역시 유성구 대학로',
      ]),
      detailAddress: `${randomIntBetween(1, 30)}층 ${randomIntBetween(100, 999)}호`,
    }), { headers: authHeaders(token) })
  );

  if (!res) return false;

  const ok = check(res, { '배송지 등록 성공': (r) => r.status === 200 || r.status === 201 });
  errorRate.add(!ok);
  return ok;
}

function createOrder(memberId, items, shippingFee) {
  const totalAmount = items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);

  const res = safeRequest(() =>
    http.post(`${BASE_URL}/orders`, JSON.stringify({
      memberId: memberId,
      sellerId: 1,
      items: items,
      receiverName: '테스터',
      receiverPhone: `010${randomIntBetween(10000000, 99999999)}`,
      zipCode: '06035',
      address: '서울특별시 강남구',
      detailAddress: '테스트빌딩 1층',
      shippingFee: shippingFee !== undefined ? shippingFee : (totalAmount >= 50000 ? 0 : 3000),
    }), { headers: memberHeaders(memberId) })
  );

  if (!res) return null;

  const ok = check(res, { '주문 생성 성공': (r) => r.status === 200 || r.status === 201 });
  errorRate.add(!ok);

  if (ok) {
    conversionFunnel.add(4); // step 4: 주문
    orderCompleted.add(1);
    revenueTotal.add(totalAmount);
    try { return JSON.parse(res.body).data; }
    catch (e) { return null; }
  }
  return null;
}

function confirmPayment(orderId, amount) {
  const paymentKey = `k6-pay-${Date.now()}-${randomIntBetween(1000, 9999)}`;

  const res = safeRequest(() =>
    http.post(`${BASE_URL}/payments/confirm`, JSON.stringify({
      paymentKey: paymentKey,
      orderId: orderId,
      amount: amount,
    }), { headers: defaultHeaders })
  );

  if (!res) return false;

  const ok = check(res, { '결제 승인 성공': (r) => r.status === 200 });
  errorRate.add(!ok);
  if (ok) conversionFunnel.add(5); // step 5: 결제 완료
  return ok;
}

function pickRandomProduct(products) {
  if (!products || products.length === 0) return null;
  return products[randomIntBetween(0, products.length - 1)];
}

function getProductOption(detail) {
  if (!detail || !detail.options || detail.options.length === 0) return null;
  return detail.options[randomIntBetween(0, detail.options.length - 1)];
}

function getUnitPrice(product) {
  return product.salePrice || product.basePrice || 29900;
}

// ============================================================
// 시나리오 1: 윈도우 쇼퍼 (30%) — 구경만 하고 이탈
// 메인 → 카테고리 2~3개 탐색 → 상품 3~5개 상세 → 이탈
// ============================================================

export function windowShopper() {
  const sessionStart = Date.now();

  group('윈도우 쇼퍼', () => {
    // 1. 메인 페이지 (상품 목록)
    const mainProducts = browseProducts(0, 12);
    sleep(randomIntBetween(3, 7));

    // 2. 카테고리 2~3개 탐색
    const categoriesToBrowse = randomIntBetween(2, 3);
    for (let i = 0; i < categoriesToBrowse; i++) {
      const catId = randomItem(CATEGORIES);
      browseProducts(0, 10, `categoryId=${catId}`);
      sleep(randomIntBetween(3, 8));
    }

    // 3. 가끔 검색 시도 (30% 확률)
    if (Math.random() < 0.3) {
      searchProducts(randomItem(SEARCH_KEYWORDS));
      sleep(randomIntBetween(3, 6));
    }

    // 4. 상품 3~5개 상세 보기
    const products = browseProducts(randomIntBetween(0, 2), 10);
    const viewCount = randomIntBetween(3, 5);
    for (let i = 0; i < Math.min(viewCount, products.length); i++) {
      viewProductDetail(products[i].id);
      sleep(randomIntBetween(3, 10)); // 상세 페이지 체류
    }

    // 5. 한 페이지 더 둘러보고 이탈
    browseProducts(randomIntBetween(1, 3), 10);
    sleep(randomIntBetween(3, 5));
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 2: 비교 쇼퍼 (15%) — 꼼꼼히 비교, 결국 안 삼
// 같은 카테고리에서 상품 5~8개 비교, 필터/정렬 적극 사용
// ============================================================

export function comparisonShopper() {
  const sessionStart = Date.now();

  group('비교 쇼퍼', () => {
    // 1. 카테고리 선택
    browseCategories();
    sleep(randomIntBetween(2, 4));

    const targetCategory = randomItem(CATEGORIES);

    // 2. 카테고리 필터로 상품 목록
    let products = filterProducts(targetCategory, null, null, null);
    sleep(randomIntBetween(3, 5));

    // 3. 브랜드 필터 적용
    const targetBrand = randomItem(BRAND_IDS);
    products = filterProducts(targetCategory, targetBrand, null, null);
    sleep(randomIntBetween(2, 4));

    // 4. 가격대 필터 적용
    const priceRange = randomItem(PRICE_RANGES);
    products = filterProducts(targetCategory, null, priceRange, null);
    sleep(randomIntBetween(2, 4));

    // 5. 정렬 변경 (최신순 → 가격순 → 인기순)
    const sortsToTry = randomIntBetween(2, 3);
    for (let i = 0; i < sortsToTry; i++) {
      const sort = randomItem(SORT_OPTIONS);
      products = filterProducts(targetCategory, null, null, sort);
      sleep(randomIntBetween(2, 4));
    }

    // 6. 상품 5~8개 상세 비교
    const compareCount = randomIntBetween(5, 8);
    for (let i = 0; i < Math.min(compareCount, products.length); i++) {
      viewProductDetail(products[i].id);
      sleep(randomIntBetween(5, 12)); // 꼼꼼히 읽는 시간
    }

    // 7. 다시 목록으로 돌아가서 한번 더 정렬 변경
    filterProducts(targetCategory, null, null, randomItem(SORT_OPTIONS));
    sleep(randomIntBetween(3, 5));

    // 8. 결국 이탈
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 3: 충동 구매자 (10%) — 빠르게 보고 바로 구매
// 메인 → 상품 1개 → 회원가입 → 배송지 → 장바구니 → 주문 → 결제
// ============================================================

export function impulseBuyer() {
  const sessionStart = Date.now();

  group('충동 구매자', () => {
    // 1. 메인 페이지에서 상품 발견
    const products = browseProducts(0, 10);
    sleep(randomIntBetween(1, 2));

    if (products.length === 0) { sleep(5); return; }

    // 2. 하나 클릭해서 상세 보기
    const product = pickRandomProduct(products);
    const detail = viewProductDetail(product.id);
    sleep(randomIntBetween(1, 3));

    if (!detail) { sleep(5); return; }
    const option = getProductOption(detail);
    if (!option) { sleep(5); return; }

    // 3. 마음에 든다! 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    sleep(randomIntBetween(1, 2));

    // 4. 배송지 등록
    registerAddress(auth.token, '테스터');
    sleep(randomIntBetween(1, 2));

    // 5. 장바구니 담기
    const unitPrice = getUnitPrice(product);
    const quantity = 1;
    addToCart(auth.memberId, product.id, option.id, quantity, unitPrice);
    sleep(randomIntBetween(1, 2));

    // 6. 주문 생성
    const orderData = createOrder(auth.memberId, [{
      productId: product.id,
      productOptionId: option.id,
      productName: product.name,
      optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
      quantity: quantity,
      unitPrice: unitPrice,
    }], unitPrice * quantity >= 50000 ? 0 : 3000);
    sleep(randomIntBetween(1, 2));

    // 7. 결제
    if (orderData && orderData.id) {
      confirmPayment(orderData.id, unitPrice * quantity);
      sleep(randomIntBetween(1, 3));

      // 주문 확인
      safeRequest(() => http.get(`${BASE_URL}/orders/${orderData.id}`, {
        headers: memberHeaders(auth.memberId),
      }));
      pageViews.add(1);
    }

    orderDuration.add(Date.now() - sessionStart);
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 4: 장바구니 모아두기 (15%) — 담고 이탈
// 로그인 → 여러 카테고리에서 3~5개 장바구니 → 주문 안 함 → 나중에 장바구니 확인
// ============================================================

export function cartAbandoner() {
  const sessionStart = Date.now();

  group('장바구니 모아두기', () => {
    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    sleep(randomIntBetween(2, 4));

    // 2. 여러 카테고리 탐색하며 장바구니 담기
    const itemsToAdd = randomIntBetween(3, 5);
    for (let i = 0; i < itemsToAdd; i++) {
      // 카테고리 변경
      const catId = randomItem(CATEGORIES);
      const products = browseProducts(0, 10, `categoryId=${catId}`);
      sleep(randomIntBetween(2, 4));

      if (products.length === 0) continue;

      // 상품 상세
      const product = pickRandomProduct(products);
      const detail = viewProductDetail(product.id);
      sleep(randomIntBetween(3, 6));

      if (!detail) continue;
      const option = getProductOption(detail);
      if (!option) continue;

      // 장바구니 담기
      addToCart(auth.memberId, product.id, option.id, randomIntBetween(1, 2), getUnitPrice(product));
      sleep(randomIntBetween(1, 3));
    }

    // 3. 장바구니 확인
    viewCart(auth.memberId);
    sleep(randomIntBetween(3, 8));

    // 4. 이탈 — 주문 안 함
    cartAbandoned.add(1);

    // 5. 잠시 후 다시 접속해서 장바구니만 확인
    sleep(randomIntBetween(10, 20));
    viewCart(auth.memberId);
    sleep(randomIntBetween(2, 5));

    // 6. 다시 이탈
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 5: 신규 회원 (10%) — 첫 방문
// 회원가입 → 배송지 2개 등록 → 카테고리/브랜드 탐색 → 위시리스트 느낌
// ============================================================

export function newMember() {
  const sessionStart = Date.now();

  group('신규 회원', () => {
    // 1. 메인 둘러보기
    browseProducts(0, 12);
    sleep(randomIntBetween(3, 6));

    // 2. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    sleep(randomIntBetween(2, 3));

    // 3. 배송지 2개 등록
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(2, 4));
    registerAddress(auth.token, '회사');
    sleep(randomIntBetween(2, 4));

    // 4. 카테고리 전체 조회
    browseCategories();
    sleep(randomIntBetween(2, 4));

    // 5. 브랜드 전체 조회
    browseBrands();
    sleep(randomIntBetween(2, 4));

    // 6. 여러 카테고리 탐색
    const categoriesToExplore = randomIntBetween(3, 5);
    for (let i = 0; i < categoriesToExplore; i++) {
      const catId = randomItem(CATEGORIES);
      const products = browseProducts(0, 10, `categoryId=${catId}`);
      sleep(randomIntBetween(2, 5));

      // 각 카테고리에서 1~2개 상세
      const viewCnt = randomIntBetween(1, 2);
      for (let j = 0; j < Math.min(viewCnt, products.length); j++) {
        viewProductDetail(products[j].id);
        sleep(randomIntBetween(3, 8));
      }
    }

    // 7. 검색도 시도
    searchProducts(randomItem(SEARCH_KEYWORDS));
    sleep(randomIntBetween(3, 6));

    // 8. 브랜드별 탐색
    const brandId = randomItem(BRAND_IDS);
    browseProducts(0, 10, `brandId=${brandId}`);
    sleep(randomIntBetween(3, 5));
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 6: 단골 구매자 (10%) — 로그인 후 바로 구매
// 이미 등록된 회원 → 로그인 → 특정 상품 → 빠르게 주문+결제
// ============================================================

export function loyalBuyer() {
  const sessionStart = Date.now();

  group('단골 구매자', () => {
    // 1. 회원가입 + 로그인 (실제로는 기존 회원이지만, 테스트 환경이므로 새로 생성)
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }

    // 배송지 등록
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(1, 2));

    // 2. 바로 검색으로 원하는 상품 찾기
    const keyword = randomItem(SEARCH_KEYWORDS);
    let products = searchProducts(keyword);
    sleep(randomIntBetween(1, 3));

    // 검색 결과 없으면 일반 목록에서
    if (products.length === 0) {
      products = browseProducts(0, 10);
      sleep(randomIntBetween(1, 2));
    }

    if (products.length === 0) { sleep(5); return; }

    // 3. 바로 상세 보기
    const product = pickRandomProduct(products);
    const detail = viewProductDetail(product.id);
    sleep(randomIntBetween(2, 4));

    if (!detail) { sleep(5); return; }
    const option = getProductOption(detail);
    if (!option) { sleep(5); return; }

    const unitPrice = getUnitPrice(product);
    const quantity = randomIntBetween(1, 2);

    // 4. 장바구니 담기
    addToCart(auth.memberId, product.id, option.id, quantity, unitPrice);
    sleep(randomIntBetween(1, 2));

    // 5. 바로 주문
    const orderData = createOrder(auth.memberId, [{
      productId: product.id,
      productOptionId: option.id,
      productName: product.name,
      optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
      quantity: quantity,
      unitPrice: unitPrice,
    }], unitPrice * quantity >= 50000 ? 0 : 3000);
    sleep(randomIntBetween(1, 2));

    // 6. 결제
    if (orderData && orderData.id) {
      confirmPayment(orderData.id, unitPrice * quantity);
      sleep(randomIntBetween(1, 2));

      // 주문 확인
      safeRequest(() => http.get(`${BASE_URL}/orders/${orderData.id}`, {
        headers: memberHeaders(auth.memberId),
      }));
      pageViews.add(1);
    }

    orderDuration.add(Date.now() - sessionStart);
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 7: 반복 방문자 (5%) — 여러 세션, 마지막에 구매
// 짧은 세션 3~4번 → 각 세션에서 1~2개 확인 → 마지막 세션에서 구매
// ============================================================

export function repeatVisitor() {
  const sessionStart = Date.now();

  group('반복 방문자', () => {
    // 먼저 회원 생성 (세션 간 동일 회원)
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(1, 2));

    let targetProduct = null;
    let targetDetail = null;

    const totalSessions = randomIntBetween(3, 4);

    for (let session = 0; session < totalSessions; session++) {
      group(`세션 ${session + 1}`, () => {
        // 짧은 탐색
        const products = browseProducts(randomIntBetween(0, 2), 10);
        sleep(randomIntBetween(2, 5));

        // 1~2개 상품 확인
        const viewCnt = randomIntBetween(1, 2);
        for (let i = 0; i < Math.min(viewCnt, products.length); i++) {
          const product = products[i];
          const detail = viewProductDetail(product.id);
          sleep(randomIntBetween(3, 8));

          // 마지막 세션 전에 마음에 드는 상품 기억
          if (detail && detail.options && detail.options.length > 0) {
            targetProduct = product;
            targetDetail = detail;
          }
        }
      });

      // 세션 간 간격 (다른 일 하는 시간)
      if (session < totalSessions - 1) {
        sleep(randomIntBetween(10, 25));
      }
    }

    // 마지막 세션: 구매!
    if (targetProduct && targetDetail) {
      group('최종 구매 세션', () => {
        const option = getProductOption(targetDetail);
        if (!option) return;

        const unitPrice = getUnitPrice(targetProduct);
        const quantity = 1;

        addToCart(auth.memberId, targetProduct.id, option.id, quantity, unitPrice);
        sleep(randomIntBetween(1, 2));

        const orderData = createOrder(auth.memberId, [{
          productId: targetProduct.id,
          productOptionId: option.id,
          productName: targetProduct.name,
          optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
          quantity: quantity,
          unitPrice: unitPrice,
        }], unitPrice * quantity >= 50000 ? 0 : 3000);
        sleep(randomIntBetween(1, 2));

        if (orderData && orderData.id) {
          confirmPayment(orderData.id, unitPrice * quantity);
        }

        orderDuration.add(Date.now() - sessionStart);
      });
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 8: 대량 구매자 (5%) — 여러 상품 한번에
// 로그인 → 장바구니에 5~10개 담기 → 주문 (5만원 이상 무료배송)
// ============================================================

export function bulkBuyer() {
  const sessionStart = Date.now();

  group('대량 구매자', () => {
    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(1, 2));

    // 2. 여러 페이지에서 상품 수집
    const allProducts = [];
    for (let page = 0; page < 3; page++) {
      const products = browseProducts(page, 10);
      allProducts.push(...products);
      sleep(randomIntBetween(1, 3));
    }

    if (allProducts.length === 0) { sleep(5); return; }

    // 3. 5~10개 상품 장바구니 담기
    const itemsToBuy = randomIntBetween(5, Math.min(10, allProducts.length));
    const orderItems = [];
    let totalAmount = 0;

    for (let i = 0; i < itemsToBuy; i++) {
      const product = allProducts[i % allProducts.length];
      const detail = viewProductDetail(product.id);
      sleep(randomIntBetween(1, 3));

      if (!detail) continue;
      const option = getProductOption(detail);
      if (!option) continue;

      const unitPrice = getUnitPrice(product);
      const quantity = randomIntBetween(1, 3);

      addToCart(auth.memberId, product.id, option.id, quantity, unitPrice);
      sleep(randomIntBetween(1, 2));

      orderItems.push({
        productId: product.id,
        productOptionId: option.id,
        productName: product.name,
        optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
        quantity: quantity,
        unitPrice: unitPrice,
      });

      totalAmount += unitPrice * quantity;
    }

    if (orderItems.length === 0) { sleep(5); return; }

    // 4. 장바구니 확인
    viewCart(auth.memberId);
    sleep(randomIntBetween(2, 5));

    // 5. 주문 생성 (5만원 이상이면 무료배송)
    const shippingFee = totalAmount >= 50000 ? 0 : 3000;
    const orderData = createOrder(auth.memberId, orderItems, shippingFee);
    sleep(randomIntBetween(1, 3));

    // 6. 결제
    if (orderData && orderData.id) {
      confirmPayment(orderData.id, totalAmount + shippingFee);
      sleep(randomIntBetween(1, 3));

      // 주문 확인
      safeRequest(() => http.get(`${BASE_URL}/orders/${orderData.id}`, {
        headers: memberHeaders(auth.memberId),
      }));
      pageViews.add(1);
    }

    orderDuration.add(Date.now() - sessionStart);
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}
