import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ============================================================
// Closet E-Commerce — 15가지 유저 행동 패턴 트래픽 시뮬레이터
//
// 시나리오:
//   1. 윈도우 쇼퍼 (20%) — 구경만 하고 이탈
//   2. 비교 쇼퍼 (11%) — 꼼꼼히 비교, 결국 안 삼
//   3. 충동 구매자 (7%) — 빠르게 보고 바로 구매
//   4. 장바구니 모아두기 (11%) — 담고 이탈
//   5. 신규 회원 (7%) — 첫 방문, 탐색 위주
//   6. 단골 구매자 (7%) — 로그인 후 바로 구매
//   7. 반복 방문자 (3%) — 여러 세션, 마지막에 구매
//   8. 대량 구매자 (3%) — 많이 담고 한번에 주문
//   9. 셀러 입점 + 상품 등록 (4%) — 셀러 계정 생성, 상품 CRUD
//  10. 리뷰 작성자 (4%) — 리뷰 작성, 투표, 요약 조회
//  11. 배송 관리자 (3%) — 배송/반품 생성, 상태 업데이트
//  12. 검색 유저 (7%) — 키워드/필터/자동완성 검색
//  13. 재고 관리자 (3%) — 입고/예약/차감/해제
//  14. 주문 취소 + 환불 (3%) — 구매 후 취소, 환불 확인
//  15. BFF 통합 유저 (4%) — BFF 엔드포인트 통합 조회
//
// 사용법:
//   k6 run load-test/realistic-traffic.js                        # 기본 (~75 VU, 10분)
//   k6 run -e DURATION=1h load-test/realistic-traffic.js         # ~75 VU, 1시간
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

// 신규 시나리오 메트릭
const reviewsCreated = new Counter('reviews_created');
const shipmentsCreated = new Counter('shipments_created');
const searchQueries = new Counter('search_queries');
const productsRegistered = new Counter('products_registered');
const inventoryOperations = new Counter('inventory_operations');
const ordersCancelled = new Counter('orders_cancelled');
const returnsRequested = new Counter('returns_requested');

// ============================================================
// 시나리오 설정 — ~75 VU 총합
// ============================================================
export const options = {
  scenarios: {
    // ── 기존 8개 시나리오 ──
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
    // ── 신규 7개 시나리오 ──
    seller_operations: {
      executor: 'constant-vus',
      vus: 3,
      duration: __ENV.DURATION || '10m',
      exec: 'sellerOperations',
    },
    review_writers: {
      executor: 'constant-vus',
      vus: 3,
      duration: __ENV.DURATION || '10m',
      exec: 'reviewWriter',
    },
    shipping_managers: {
      executor: 'constant-vus',
      vus: 2,
      duration: __ENV.DURATION || '10m',
      exec: 'shippingManager',
    },
    search_users: {
      executor: 'constant-vus',
      vus: 5,
      duration: __ENV.DURATION || '10m',
      exec: 'searchUser',
    },
    inventory_managers: {
      executor: 'constant-vus',
      vus: 2,
      duration: __ENV.DURATION || '10m',
      exec: 'inventoryManager',
    },
    order_cancellers: {
      executor: 'constant-vus',
      vus: 2,
      duration: __ENV.DURATION || '10m',
      exec: 'orderCanceller',
    },
    bff_users: {
      executor: 'constant-vus',
      vus: 3,
      duration: __ENV.DURATION || '10m',
      exec: 'bffUser',
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
const SEARCH_KEYWORDS_V2 = ['무신사', '나이키', '반팔', '데님', '스니커즈', '후드', '맨투맨', '코트', '슬랙스', '백팩'];
const AUTOCOMPLETE_PREFIXES = ['무', '무신', '나이', '반', '데', '스니', '후', '맨', '코', '백'];
const PRODUCT_NAMES = ['오버핏 반팔티', '와이드 데님', '캐주얼 자켓', '스포츠 후드', '슬림 슬랙스', '레더 스니커즈', '울 코트', '니트 가디건', '조거팬츠', '패딩 점퍼'];
const SIZES = ['S', 'M', 'L', 'XL'];
const COLORS = ['블랙', '화이트', '네이비', '그레이', '베이지'];
const CARRIERS = ['CJ', 'LOGEN', 'HANJIN', 'LOTTE', 'POST'];

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

// ============================================================
// 시나리오 9: 셀러 입점 + 상품 등록 (4%) — 셀러 계정 생성, 상품 CRUD
// 회원가입(셀러) → 상품 등록(DRAFT) → 옵션 추가 → 상품 활성화 → 수정/판매중지
// ============================================================

export function sellerOperations() {
  const sessionStart = Date.now();

  group('셀러 입점 + 상품 등록', () => {
    // 1. 셀러 회원가입
    const uniqueId = `${Date.now()}-${randomIntBetween(1000, 9999)}`;
    const email = `seller-${uniqueId}@loadtest.com`;

    const registerRes = safeRequest(() =>
      http.post(`${BASE_URL}/members/register`, JSON.stringify({
        email: email,
        password: 'password123',
        name: `셀러${uniqueId}`,
        phone: `010${randomIntBetween(10000000, 99999999)}`,
      }), { headers: defaultHeaders })
    );

    if (!registerRes || (registerRes.status !== 200 && registerRes.status !== 201)) {
      errorRate.add(1);
      sleep(5);
      return;
    }
    signupCount.add(1);
    sleep(randomIntBetween(2, 4));

    // 2. 상품 등록 (DRAFT) — 2~3개 상품
    const productCount = randomIntBetween(2, 3);
    const createdProducts = [];

    for (let i = 0; i < productCount; i++) {
      const productName = `${randomItem(PRODUCT_NAMES)}-${uniqueId}-${i}`;
      const basePrice = randomIntBetween(15000, 120000);

      const productRes = safeRequest(() =>
        http.post(`${BASE_URL}/products`, JSON.stringify({
          name: productName,
          brandId: randomItem(BRAND_IDS),
          categoryId: randomItem(CATEGORIES),
          basePrice: basePrice,
          salePrice: Math.floor(basePrice * (randomIntBetween(70, 95) / 100)),
          description: `${productName} 상품 설명입니다. 고급 소재를 사용하여 편안한 착용감을 제공합니다.`,
          material: randomItem(['면 100%', '폴리에스터 100%', '면 60% 폴리에스터 40%', '나일론 100%']),
          season: randomItem(['SS', 'FW', 'ALL']),
          gender: randomItem(['MALE', 'FEMALE', 'UNISEX']),
        }), { headers: defaultHeaders })
      );

      if (productRes && (productRes.status === 200 || productRes.status === 201)) {
        productsRegistered.add(1);
        try {
          const productData = JSON.parse(productRes.body).data;
          if (productData && productData.id) {
            createdProducts.push(productData);
          }
        } catch (e) { /* ignore parse error */ }
      }
      sleep(randomIntBetween(2, 4));

      // 3. 옵션 추가 (사이즈 x 색상 2~3개)
      if (createdProducts.length > 0) {
        const lastProduct = createdProducts[createdProducts.length - 1];
        const optionCount = randomIntBetween(2, 3);

        for (let j = 0; j < optionCount; j++) {
          const optionRes = safeRequest(() =>
            http.post(`${BASE_URL}/products/${lastProduct.id}/options`, JSON.stringify({
              size: randomItem(SIZES),
              colorName: randomItem(COLORS),
              additionalPrice: randomIntBetween(0, 3000),
              stockQuantity: randomIntBetween(20, 100),
            }), { headers: defaultHeaders })
          );

          if (optionRes) {
            check(optionRes, { '옵션 추가 성공': (r) => r.status === 200 || r.status === 201 });
          }
          sleep(randomIntBetween(1, 2));
        }

        // 4. 상품 활성화 (DRAFT → ACTIVE)
        const activateRes = safeRequest(() =>
          http.patch(`${BASE_URL}/products/${lastProduct.id}/status`, JSON.stringify({
            status: 'ACTIVE',
          }), { headers: defaultHeaders })
        );

        if (activateRes) {
          check(activateRes, { '상품 활성화 성공': (r) => r.status === 200 });
        }
        sleep(randomIntBetween(2, 4));

        // 5. 가끔 가격 수정 (40% 확률)
        if (Math.random() < 0.4) {
          const newPrice = randomIntBetween(10000, 150000);
          const updateRes = safeRequest(() =>
            http.put(`${BASE_URL}/products/${lastProduct.id}`, JSON.stringify({
              name: lastProduct.name,
              basePrice: newPrice,
              salePrice: Math.floor(newPrice * 0.85),
            }), { headers: defaultHeaders })
          );

          if (updateRes) {
            check(updateRes, { '상품 수정 성공': (r) => r.status === 200 });
          }
          sleep(randomIntBetween(2, 3));
        }

        // 6. 가끔 판매중지 (20% 확률)
        if (Math.random() < 0.2) {
          const inactiveRes = safeRequest(() =>
            http.patch(`${BASE_URL}/products/${lastProduct.id}/status`, JSON.stringify({
              status: 'INACTIVE',
            }), { headers: defaultHeaders })
          );

          if (inactiveRes) {
            check(inactiveRes, { '상품 판매중지 성공': (r) => r.status === 200 });
          }
          sleep(randomIntBetween(2, 4));
        }
      }
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 10: 리뷰 작성자 (4%) — 리뷰 작성, 투표, 요약 조회
// 로그인 → 상품 상세 → 리뷰 작성 → 도움돼요 투표 → 리뷰 목록/요약 조회
// ============================================================

export function reviewWriter() {
  const sessionStart = Date.now();

  group('리뷰 작성자', () => {
    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    sleep(randomIntBetween(2, 3));

    // 2. 상품 목록에서 상품 선택
    const products = browseProducts(0, 10);
    if (products.length === 0) { sleep(5); return; }
    sleep(randomIntBetween(2, 4));

    // 3. 상품 상세 보기
    const product = pickRandomProduct(products);
    viewProductDetail(product.id);
    sleep(randomIntBetween(3, 6));

    // 4. 리뷰 작성 (unique orderItemId로 중복 방지)
    const orderItemId = Date.now() * 1000 + randomIntBetween(1, 999);
    const rating = randomIntBetween(1, 5);
    const sizeFeeling = randomItem(['SMALL', 'TRUE_TO_SIZE', 'LARGE']);
    const height = randomIntBetween(155, 190);
    const weight = randomIntBetween(45, 95);
    const contents = [
      '사이즈 딱 맞아요! 재질도 좋고 색감도 사진이랑 동일합니다.',
      '배송 빠르고 품질 좋습니다. 다음에도 또 구매할게요.',
      '약간 크게 나왔지만 전체적으로 만족합니다.',
      '생각보다 얇아요. 봄가을에 입기 좋을 듯합니다.',
      '색상이 사진보다 좀 어두워요. 그래도 괜찮습니다.',
      '핏이 예쁘고 소재가 부드러워요. 강추!',
      '가격 대비 훌륭합니다. 친구한테도 추천했어요.',
    ];

    const reviewRes = safeRequest(() =>
      http.post(`${BASE_URL}/reviews`, JSON.stringify({
        productId: product.id,
        orderItemId: orderItemId,
        rating: rating,
        content: randomItem(contents),
        height: height,
        weight: weight,
        sizeFeeling: sizeFeeling,
      }), { headers: memberHeaders(auth.memberId) })
    );

    let reviewId = null;
    if (reviewRes && (reviewRes.status === 200 || reviewRes.status === 201)) {
      reviewsCreated.add(1);
      try {
        reviewId = JSON.parse(reviewRes.body).data?.id;
      } catch (e) { /* ignore */ }
    }
    sleep(randomIntBetween(2, 4));

    // 5. 리뷰 도움돼요 투표
    if (reviewId) {
      const helpfulRes = safeRequest(() =>
        http.post(`${BASE_URL}/reviews/${reviewId}/helpful`, JSON.stringify({}), {
          headers: memberHeaders(auth.memberId),
        })
      );
      if (helpfulRes) {
        check(helpfulRes, { '리뷰 도움돼요 투표': (r) => r.status === 200 || r.status === 201 || r.status === 409 });
      }
      sleep(randomIntBetween(1, 3));
    }

    // 6. 상품별 리뷰 목록 조회
    const reviewListRes = safeRequest(() =>
      http.get(`${BASE_URL}/reviews/products/${product.id}?page=0&size=10`)
    );
    if (reviewListRes) {
      check(reviewListRes, { '리뷰 목록 조회 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(2, 5));

    // 7. 리뷰 요약 조회
    const summaryRes = safeRequest(() =>
      http.get(`${BASE_URL}/reviews/products/${product.id}/summary`)
    );
    if (summaryRes) {
      check(summaryRes, { '리뷰 요약 조회 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 8. 다른 상품 리뷰도 조회 (50% 확률)
    if (Math.random() < 0.5 && products.length > 1) {
      const otherProduct = products[randomIntBetween(0, products.length - 1)];
      safeRequest(() =>
        http.get(`${BASE_URL}/reviews/products/${otherProduct.id}?page=0&size=10`)
      );
      pageViews.add(1);
      sleep(randomIntBetween(2, 4));
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 11: 배송 관리자 (3%) — 배송/반품 생성, 상태 업데이트
// 주문 생성 → 배송 생성 → 송장 등록 → 상태 진행 → 반품 신청/승인
// ============================================================

export function shippingManager() {
  const sessionStart = Date.now();

  group('배송 관리자', () => {
    // 1. 주문 생성을 위해 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(1, 2));

    // 2. 상품 선택 + 주문 생성
    const products = browseProducts(0, 10);
    if (products.length === 0) { sleep(5); return; }

    const product = pickRandomProduct(products);
    const detail = viewProductDetail(product.id);
    if (!detail) { sleep(5); return; }

    const option = getProductOption(detail);
    if (!option) { sleep(5); return; }

    const unitPrice = getUnitPrice(product);
    const orderData = createOrder(auth.memberId, [{
      productId: product.id,
      productOptionId: option.id,
      productName: product.name,
      optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
      quantity: 1,
      unitPrice: unitPrice,
    }], unitPrice >= 50000 ? 0 : 3000);

    if (!orderData || !orderData.id) { sleep(5); return; }
    sleep(randomIntBetween(1, 3));

    // 3. 결제 확정
    confirmPayment(orderData.id, unitPrice);
    sleep(randomIntBetween(2, 4));

    // 4. 배송 생성
    const shipmentRes = safeRequest(() =>
      http.post(`${BASE_URL}/shipments`, JSON.stringify({
        orderId: orderData.id,
        sellerId: 1,
        receiverName: '테스터',
        receiverPhone: `010${randomIntBetween(10000000, 99999999)}`,
        receiverAddress: '서울특별시 강남구 테헤란로 123',
      }), { headers: defaultHeaders })
    );

    let shipmentId = null;
    if (shipmentRes && (shipmentRes.status === 200 || shipmentRes.status === 201)) {
      shipmentsCreated.add(1);
      try {
        shipmentId = JSON.parse(shipmentRes.body).data?.id;
      } catch (e) { /* ignore */ }
    }
    sleep(randomIntBetween(2, 4));

    if (!shipmentId) { sleep(5); return; }

    // 5. 송장 등록
    const trackingNumber = `${randomIntBetween(100000000000, 999999999999)}`;
    const trackingRes = safeRequest(() =>
      http.patch(`${BASE_URL}/shipments/${shipmentId}/tracking`, JSON.stringify({
        carrier: randomItem(CARRIERS),
        trackingNumber: trackingNumber,
      }), { headers: defaultHeaders })
    );

    if (trackingRes) {
      check(trackingRes, { '송장 등록 성공': (r) => r.status === 200 });
    }
    sleep(randomIntBetween(2, 4));

    // 6. 배송 상태 업데이트 (PENDING → READY → PICKED_UP → IN_TRANSIT → DELIVERED)
    const statuses = ['READY', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'];
    for (const status of statuses) {
      const statusRes = safeRequest(() =>
        http.patch(`${BASE_URL}/shipments/${shipmentId}/status`, JSON.stringify({
          status: status,
        }), { headers: defaultHeaders })
      );

      if (statusRes) {
        check(statusRes, { [`배송 상태 ${status}`]: (r) => r.status === 200 });
      }
      sleep(randomIntBetween(2, 5));
    }

    // 7. 반품 신청 (50% 확률)
    if (Math.random() < 0.5) {
      const orderItemId = Date.now() * 1000 + randomIntBetween(1, 999);
      const returnRes = safeRequest(() =>
        http.post(`${BASE_URL}/returns`, JSON.stringify({
          orderId: orderData.id,
          orderItemId: orderItemId,
          type: 'RETURN',
          reasonType: randomItem(['CHANGE_OF_MIND', 'DEFECTIVE', 'WRONG_ITEM', 'SIZE_ISSUE']),
          reason: '사이즈가 맞지 않아서 반품 요청합니다.',
        }), { headers: memberHeaders(auth.memberId) })
      );

      let returnId = null;
      if (returnRes && (returnRes.status === 200 || returnRes.status === 201)) {
        returnsRequested.add(1);
        try {
          returnId = JSON.parse(returnRes.body).data?.id;
        } catch (e) { /* ignore */ }
      }
      sleep(randomIntBetween(2, 4));

      // 8. 반품 승인 (70% 확률)
      if (returnId && Math.random() < 0.7) {
        const approveRes = safeRequest(() =>
          http.patch(`${BASE_URL}/returns/${returnId}/approve`, JSON.stringify({}), {
            headers: defaultHeaders,
          })
        );

        if (approveRes) {
          check(approveRes, { '반품 승인 성공': (r) => r.status === 200 });
        }
        sleep(randomIntBetween(2, 4));
      }
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 12: 검색 유저 (7%) — 키워드/필터/자동완성 검색
// 키워드 검색 → 필터 검색 → 자동완성 → 상품 상세 클릭 → 연속 검색
// ============================================================

export function searchUser() {
  const sessionStart = Date.now();

  group('검색 유저', () => {
    // 1. 키워드 검색
    const keyword = randomItem(SEARCH_KEYWORDS_V2);
    const searchRes = safeRequest(() =>
      http.get(`${BASE_URL}/search/products?keyword=${encodeURIComponent(keyword)}&page=0&size=10`)
    );

    let searchResults = [];
    if (searchRes) {
      const ok = check(searchRes, { '키워드 검색 성공': (r) => r.status === 200 });
      searchQueries.add(1);
      errorRate.add(!ok);
      if (ok) {
        try { searchResults = JSON.parse(searchRes.body).data?.content || []; }
        catch (e) { /* ignore */ }
      }
    }
    sleep(randomIntBetween(2, 5));

    // 2. 필터 검색 (카테고리 + 브랜드 + 가격대)
    const catId = randomItem(CATEGORIES);
    const brandId = randomItem(BRAND_IDS);
    const priceRange = randomItem(PRICE_RANGES);
    const filterRes = safeRequest(() =>
      http.get(`${BASE_URL}/search/products?categoryId=${catId}&brandId=${brandId}&minPrice=${priceRange.min}&maxPrice=${priceRange.max}&page=0&size=10`)
    );

    if (filterRes) {
      check(filterRes, { '필터 검색 성공': (r) => r.status === 200 });
      searchQueries.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 3. 자동완성 — 점진적 타이핑 시뮬레이션
    const prefix = randomItem(AUTOCOMPLETE_PREFIXES);
    const autocompleteRes = safeRequest(() =>
      http.get(`${BASE_URL}/search/autocomplete?q=${encodeURIComponent(prefix)}&limit=5`)
    );

    if (autocompleteRes) {
      check(autocompleteRes, { '자동완성 성공': (r) => r.status === 200 });
      searchQueries.add(1);
    }
    sleep(randomIntBetween(1, 3));

    // 4. 검색 결과에서 상품 상세 클릭
    if (searchResults.length > 0) {
      const clickedProduct = searchResults[randomIntBetween(0, searchResults.length - 1)];
      if (clickedProduct && clickedProduct.id) {
        viewProductDetail(clickedProduct.id);
        sleep(randomIntBetween(3, 6));
      }
    } else {
      // 검색 결과 없으면 일반 상품 목록에서 클릭
      const fallbackProducts = browseProducts(0, 5);
      if (fallbackProducts.length > 0) {
        viewProductDetail(fallbackProducts[0].id);
        sleep(randomIntBetween(3, 6));
      }
    }

    // 5. 연속 검색 (키워드 변경) — 2~3회 추가
    const additionalSearches = randomIntBetween(2, 3);
    for (let i = 0; i < additionalSearches; i++) {
      const nextKeyword = randomItem(SEARCH_KEYWORDS_V2);
      const nextRes = safeRequest(() =>
        http.get(`${BASE_URL}/search/products?keyword=${encodeURIComponent(nextKeyword)}&page=0&size=10`)
      );

      if (nextRes) {
        check(nextRes, { '연속 검색 성공': (r) => r.status === 200 });
        searchQueries.add(1);
      }
      sleep(randomIntBetween(2, 5));

      // 자동완성도 추가
      const nextPrefix = randomItem(AUTOCOMPLETE_PREFIXES);
      safeRequest(() =>
        http.get(`${BASE_URL}/search/autocomplete?q=${encodeURIComponent(nextPrefix)}&limit=5`)
      );
      searchQueries.add(1);
      sleep(randomIntBetween(1, 2));
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 13: 재고 관리자 (3%) — 입고/예약/차감/해제
// 재고 입고 → 재고 조회 → 대량 조회 → 예약 → 해제 → 차감
// ============================================================

export function inventoryManager() {
  const sessionStart = Date.now();

  group('재고 관리자', () => {
    // 재고 작업 대상 옵션 ID들
    const optionIds = [
      randomIntBetween(1, 20),
      randomIntBetween(1, 20),
      randomIntBetween(1, 20),
    ];

    // 1. 재고 입고 (restock)
    for (const optionId of optionIds) {
      const restockRes = safeRequest(() =>
        http.post(`${BASE_URL}/inventory/restock`, JSON.stringify({
          productOptionId: optionId,
          quantity: randomIntBetween(50, 200),
        }), { headers: defaultHeaders })
      );

      if (restockRes) {
        check(restockRes, { '재고 입고 성공': (r) => r.status === 200 || r.status === 201 });
        inventoryOperations.add(1);
      }
      sleep(randomIntBetween(1, 3));
    }

    // 2. 개별 재고 조회
    for (const optionId of optionIds) {
      const invRes = safeRequest(() =>
        http.get(`${BASE_URL}/inventory/${optionId}`)
      );

      if (invRes) {
        check(invRes, { '재고 조회 성공': (r) => r.status === 200 });
        inventoryOperations.add(1);
      }
      sleep(randomIntBetween(1, 2));
    }

    // 3. 대량 재고 조회
    const bulkIds = optionIds.join(',');
    const bulkRes = safeRequest(() =>
      http.get(`${BASE_URL}/inventory/bulk?ids=${bulkIds}`)
    );

    if (bulkRes) {
      check(bulkRes, { '대량 재고 조회 성공': (r) => r.status === 200 });
      inventoryOperations.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 4. 재고 예약 (주문 시뮬레이션)
    const reserveOptionId = randomItem(optionIds);
    const reserveQuantity = randomIntBetween(1, 5);
    const fakeOrderId = `inv-order-${Date.now()}-${randomIntBetween(1000, 9999)}`;

    const reserveRes = safeRequest(() =>
      http.post(`${BASE_URL}/inventory/reserve`, JSON.stringify({
        productOptionId: reserveOptionId,
        quantity: reserveQuantity,
        orderId: fakeOrderId,
      }), { headers: defaultHeaders })
    );

    if (reserveRes) {
      check(reserveRes, { '재고 예약 성공': (r) => r.status === 200 || r.status === 201 });
      inventoryOperations.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 5. 재고 해제 (50%) 또는 차감 (50%)
    if (Math.random() < 0.5) {
      const releaseRes = safeRequest(() =>
        http.post(`${BASE_URL}/inventory/release`, JSON.stringify({
          productOptionId: reserveOptionId,
          quantity: reserveQuantity,
          orderId: fakeOrderId,
        }), { headers: defaultHeaders })
      );

      if (releaseRes) {
        check(releaseRes, { '재고 해제 성공': (r) => r.status === 200 });
        inventoryOperations.add(1);
      }
    } else {
      const deductRes = safeRequest(() =>
        http.post(`${BASE_URL}/inventory/deduct`, JSON.stringify({
          productOptionId: reserveOptionId,
          quantity: reserveQuantity,
          orderId: fakeOrderId,
        }), { headers: defaultHeaders })
      );

      if (deductRes) {
        check(deductRes, { '재고 차감 성공': (r) => r.status === 200 });
        inventoryOperations.add(1);
      }
    }
    sleep(randomIntBetween(2, 5));

    // 6. 최종 재고 확인
    for (const optionId of optionIds) {
      safeRequest(() => http.get(`${BASE_URL}/inventory/${optionId}`));
      inventoryOperations.add(1);
      sleep(randomIntBetween(1, 2));
    }
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 14: 주문 취소 + 환불 유저 (3%) — 구매 후 취소, 환불 확인
// 회원가입 → 로그인 → 상품 구매 → 주문 취소 → 환불 상태 확인
// ============================================================

export function orderCanceller() {
  const sessionStart = Date.now();

  group('주문 취소 + 환불', () => {
    // 1. 회원가입 + 로그인
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    registerAddress(auth.token, '집');
    sleep(randomIntBetween(1, 2));

    // 2. 상품 선택
    const products = browseProducts(0, 10);
    if (products.length === 0) { sleep(5); return; }

    const product = pickRandomProduct(products);
    const detail = viewProductDetail(product.id);
    if (!detail) { sleep(5); return; }

    const option = getProductOption(detail);
    if (!option) { sleep(5); return; }

    const unitPrice = getUnitPrice(product);
    const quantity = randomIntBetween(1, 2);
    sleep(randomIntBetween(1, 2));

    // 3. 장바구니 + 주문
    addToCart(auth.memberId, product.id, option.id, quantity, unitPrice);
    sleep(randomIntBetween(1, 2));

    const orderData = createOrder(auth.memberId, [{
      productId: product.id,
      productOptionId: option.id,
      productName: product.name,
      optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
      quantity: quantity,
      unitPrice: unitPrice,
    }], unitPrice * quantity >= 50000 ? 0 : 3000);

    if (!orderData || !orderData.id) { sleep(5); return; }
    sleep(randomIntBetween(1, 2));

    // 4. 결제
    confirmPayment(orderData.id, unitPrice * quantity);
    sleep(randomIntBetween(3, 6));

    // 5. 주문 취소
    const cancelReasons = [
      '단순 변심으로 취소합니다.',
      '다른 상품으로 변경하려고 합니다.',
      '배송이 너무 오래 걸려서 취소합니다.',
      '가격이 더 싼 곳을 찾았습니다.',
    ];

    const cancelRes = safeRequest(() =>
      http.post(`${BASE_URL}/orders/${orderData.id}/cancel`, JSON.stringify({
        reason: randomItem(cancelReasons),
      }), { headers: memberHeaders(auth.memberId) })
    );

    if (cancelRes) {
      const ok = check(cancelRes, { '주문 취소 성공': (r) => r.status === 200 });
      if (ok) ordersCancelled.add(1);
      errorRate.add(!ok);
    }
    sleep(randomIntBetween(3, 6));

    // 6. 환불 상태 확인
    const refundRes = safeRequest(() =>
      http.get(`${BASE_URL}/payments/orders/${orderData.id}`, {
        headers: memberHeaders(auth.memberId),
      })
    );

    if (refundRes) {
      check(refundRes, { '환불 상태 조회 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 7. 주문 상세도 확인
    const orderCheckRes = safeRequest(() =>
      http.get(`${BASE_URL}/orders/${orderData.id}`, {
        headers: memberHeaders(auth.memberId),
      })
    );

    if (orderCheckRes) {
      check(orderCheckRes, { '취소된 주문 조회': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(2, 4));
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}

// ============================================================
// 시나리오 15: BFF 통합 유저 (4%) — BFF 엔드포인트 통합 조회
// 홈 → 상품 상세 → 마이페이지 → 체크아웃 → 주문 상세
// ============================================================

export function bffUser() {
  const sessionStart = Date.now();

  group('BFF 통합 유저', () => {
    // 1. 회원가입 + 로그인 (마이페이지/체크아웃에 필요)
    const auth = registerAndLogin();
    if (!auth) { sleep(5); return; }
    sleep(randomIntBetween(1, 2));

    // 2. BFF 홈 페이지 조회
    const homeRes = safeRequest(() =>
      http.get(`${BASE_URL}/bff/home`)
    );

    if (homeRes) {
      check(homeRes, { 'BFF 홈 조회 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(3, 6));

    // 3. BFF 상품 상세 조회
    const productIds = [randomIntBetween(1, 20), randomIntBetween(1, 20)];
    for (const pid of productIds) {
      const productRes = safeRequest(() =>
        http.get(`${BASE_URL}/bff/products/${pid}`)
      );

      if (productRes) {
        check(productRes, { 'BFF 상품 상세 성공': (r) => r.status === 200 });
        pageViews.add(1);
      }
      sleep(randomIntBetween(3, 6));
    }

    // 4. BFF 마이페이지 조회
    const mypageRes = safeRequest(() =>
      http.get(`${BASE_URL}/bff/mypage`, {
        headers: memberHeaders(auth.memberId),
      })
    );

    if (mypageRes) {
      check(mypageRes, { 'BFF 마이페이지 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(3, 5));

    // 5. BFF 체크아웃 조회
    const checkoutRes = safeRequest(() =>
      http.get(`${BASE_URL}/bff/checkout`, {
        headers: memberHeaders(auth.memberId),
      })
    );

    if (checkoutRes) {
      check(checkoutRes, { 'BFF 체크아웃 성공': (r) => r.status === 200 });
      pageViews.add(1);
    }
    sleep(randomIntBetween(2, 4));

    // 6. 주문 생성 후 BFF 주문 상세 조회
    const products = browseProducts(0, 5);
    if (products.length > 0) {
      const product = pickRandomProduct(products);
      const detail = viewProductDetail(product.id);

      if (detail) {
        const option = getProductOption(detail);
        if (option) {
          const unitPrice = getUnitPrice(product);
          registerAddress(auth.token, '집');

          const orderData = createOrder(auth.memberId, [{
            productId: product.id,
            productOptionId: option.id,
            productName: product.name,
            optionName: `${option.size || 'M'}/${option.colorName || '블랙'}`,
            quantity: 1,
            unitPrice: unitPrice,
          }], unitPrice >= 50000 ? 0 : 3000);

          if (orderData && orderData.id) {
            sleep(randomIntBetween(2, 4));

            // BFF 주문 상세
            const orderDetailRes = safeRequest(() =>
              http.get(`${BASE_URL}/bff/orders/${orderData.id}`, {
                headers: memberHeaders(auth.memberId),
              })
            );

            if (orderDetailRes) {
              check(orderDetailRes, { 'BFF 주문 상세 성공': (r) => r.status === 200 });
              pageViews.add(1);
            }
            sleep(randomIntBetween(2, 4));
          }
        }
      }
    }

    // 7. 다시 홈으로
    safeRequest(() => http.get(`${BASE_URL}/bff/home`));
    pageViews.add(1);
    sleep(randomIntBetween(2, 4));
  });

  avgSessionDuration.add(Date.now() - sessionStart);
}
