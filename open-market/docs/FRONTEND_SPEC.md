# 🖥️ 프론트엔드 요구사항 (FRONTEND_SPEC.md)

> **담당**: LLM Agent
> **기술 스택**: Next.js 14+ (App Router), TypeScript, Tailwind CSS

---

## 프로젝트 구조

```
frontend/
├── src/
│   ├── app/                          # Next.js App Router
│   │   ├── (auth)/                   # 인증 라우트 그룹
│   │   │   ├── login/
│   │   │   │   └── page.tsx
│   │   │   ├── signup/
│   │   │   │   └── page.tsx
│   │   │   └── layout.tsx
│   │   │
│   │   ├── (buyer)/                  # 구매자 라우트 그룹
│   │   │   ├── page.tsx              # 메인 페이지
│   │   │   ├── products/
│   │   │   │   ├── page.tsx          # 상품 목록
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx      # 상품 상세
│   │   │   ├── cart/
│   │   │   │   └── page.tsx          # 장바구니
│   │   │   ├── orders/
│   │   │   │   ├── page.tsx          # 주문 목록
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx      # 주문 상세
│   │   │   │   └── checkout/
│   │   │   │       └── page.tsx      # 주문/결제
│   │   │   ├── mypage/
│   │   │   │   └── page.tsx          # 마이페이지
│   │   │   └── layout.tsx
│   │   │
│   │   ├── (seller)/                 # 판매자 라우트 그룹
│   │   │   ├── dashboard/
│   │   │   │   └── page.tsx          # 대시보드
│   │   │   ├── products/
│   │   │   │   ├── page.tsx          # 상품 관리
│   │   │   │   ├── new/
│   │   │   │   │   └── page.tsx      # 상품 등록
│   │   │   │   └── [id]/
│   │   │   │       └── edit/
│   │   │   │           └── page.tsx  # 상품 수정
│   │   │   ├── orders/
│   │   │   │   └── page.tsx          # 주문 관리
│   │   │   ├── settlements/
│   │   │   │   └── page.tsx          # 정산 관리
│   │   │   └── layout.tsx
│   │   │
│   │   ├── layout.tsx                # 루트 레이아웃
│   │   ├── globals.css
│   │   └── providers.tsx             # Provider 래퍼
│   │
│   ├── components/
│   │   ├── ui/                       # shadcn/ui 컴포넌트
│   │   │   ├── button.tsx
│   │   │   ├── input.tsx
│   │   │   ├── card.tsx
│   │   │   ├── dialog.tsx
│   │   │   ├── dropdown-menu.tsx
│   │   │   ├── select.tsx
│   │   │   ├── table.tsx
│   │   │   ├── toast.tsx
│   │   │   └── ...
│   │   │
│   │   ├── layout/                   # 레이아웃 컴포넌트
│   │   │   ├── Header.tsx
│   │   │   ├── Footer.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── BuyerLayout.tsx
│   │   │   └── SellerLayout.tsx
│   │   │
│   │   └── features/                 # 기능별 컴포넌트
│   │       ├── auth/
│   │       │   ├── LoginForm.tsx
│   │       │   └── SignupForm.tsx
│   │       ├── product/
│   │       │   ├── ProductCard.tsx
│   │       │   ├── ProductList.tsx
│   │       │   ├── ProductDetail.tsx
│   │       │   ├── ProductForm.tsx
│   │       │   └── ProductOptionSelector.tsx
│   │       ├── cart/
│   │       │   ├── CartItem.tsx
│   │       │   └── CartSummary.tsx
│   │       ├── order/
│   │       │   ├── OrderItem.tsx
│   │       │   ├── OrderStatus.tsx
│   │       │   └── CheckoutForm.tsx
│   │       └── seller/
│   │           ├── DashboardStats.tsx
│   │           ├── OrderTable.tsx
│   │           └── SettlementTable.tsx
│   │
│   ├── hooks/                        # Custom Hooks
│   │   ├── useAuth.ts
│   │   ├── useCart.ts
│   │   ├── useDebounce.ts
│   │   └── useInfiniteScroll.ts
│   │
│   ├── stores/                       # Zustand Stores
│   │   ├── authStore.ts
│   │   ├── cartStore.ts
│   │   └── uiStore.ts
│   │
│   ├── lib/                          # 유틸리티
│   │   ├── api/
│   │   │   ├── client.ts             # API 클라이언트
│   │   │   ├── auth.ts               # 인증 API
│   │   │   ├── products.ts           # 상품 API
│   │   │   ├── orders.ts             # 주문 API
│   │   │   └── payments.ts           # 결제 API
│   │   ├── utils/
│   │   │   ├── format.ts             # 포맷 유틸
│   │   │   └── validation.ts         # 유효성 검사
│   │   └── constants.ts              # 상수
│   │
│   └── types/                        # TypeScript 타입
│       ├── api.ts                    # API 응답 타입
│       ├── member.ts
│       ├── product.ts
│       ├── order.ts
│       └── payment.ts
│
├── public/
│   └── images/
│
├── package.json
├── tsconfig.json
├── tailwind.config.ts
├── next.config.js
└── .env.local
```

---

## 페이지별 상세 스펙

### 1. 공통

#### Header
```typescript
interface HeaderProps {
  user?: User | null;
}

// 기능
- 로고 (홈 링크)
- 검색창 (상품 검색)
- 카테고리 메뉴
- 로그인/회원가입 또는 사용자 메뉴
- 장바구니 아이콘 (수량 배지)
- 판매자 센터 링크 (판매자인 경우)
```

#### Footer
```
- 회사 정보
- 고객센터 정보
- 이용약관, 개인정보처리방침 링크
- SNS 링크
```

---

### 2. 구매자 화면

#### 메인 페이지 (/)
```typescript
// 섹션 구성
1. 히어로 배너 (캐러셀)
2. 카테고리 바로가기
3. 추천 상품 (베스트셀러)
4. 신상품
5. 카테고리별 상품

// API
GET /api/v1/products?sort=sales&limit=10  // 베스트셀러
GET /api/v1/products?sort=newest&limit=10 // 신상품
```

#### 상품 목록 (/products)
```typescript
// 기능
- 카테고리 필터 (사이드바)
- 가격 범위 필터
- 정렬 (최신순, 가격순, 판매순, 리뷰순)
- 무한 스크롤 또는 페이지네이션
- 그리드/리스트 뷰 전환

// API
GET /api/v1/products?category={id}&minPrice={}&maxPrice={}&sort={}&page={}&size={}

// 상태
interface ProductListState {
  filters: {
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
  };
  sort: 'newest' | 'price_asc' | 'price_desc' | 'sales';
  view: 'grid' | 'list';
}
```

#### 상품 상세 (/products/[id])
```typescript
// 기능
- 이미지 갤러리 (메인 이미지 + 썸네일)
- 상품 정보 (이름, 가격, 설명)
- 옵션 선택 (색상, 사이즈 등)
- 수량 선택
- 장바구니 담기 / 바로구매 버튼
- 리뷰 목록
- 상품 문의

// API
GET /api/v1/products/{id}
GET /api/v1/products/{id}/reviews
POST /api/v1/cart/items  // 장바구니 담기
```

#### 장바구니 (/cart)
```typescript
// 기능
- 상품 목록 (이미지, 이름, 옵션, 가격, 수량)
- 개별 선택/전체 선택
- 수량 변경
- 선택 삭제
- 예상 결제 금액 (상품금액, 배송비, 할인)
- 주문하기 버튼

// 상태 (Zustand)
interface CartStore {
  items: CartItem[];
  selectedIds: number[];
  
  addItem: (item: CartItem) => void;
  removeItem: (id: number) => void;
  updateQuantity: (id: number, quantity: number) => void;
  toggleSelect: (id: number) => void;
  selectAll: () => void;
  clearSelected: () => void;
  
  // Computed
  totalAmount: number;
  selectedItems: CartItem[];
}
```

#### 주문/결제 (/orders/checkout)
```typescript
// 기능
- 주문 상품 목록
- 배송지 정보 (신규 입력 / 기존 주소 선택)
- 결제 수단 선택 (토스페이먼츠, 카카오페이, 네이버페이, 다날)
- 최종 결제 금액
- 결제하기 버튼

// API
POST /api/v1/orders        // 주문 생성
POST /api/v1/payments/ready // 결제 준비

// 결제 플로우
1. 주문 생성 → orderId 발급
2. 결제 준비 → PG사 결제창 호출
3. 결제 완료 콜백 → 결제 승인 API
4. 주문 완료 페이지로 이동
```

#### 주문 내역 (/orders)
```typescript
// 기능
- 주문 목록 (최신순)
- 주문 상태 필터 (전체, 결제완료, 배송중, 배송완료, 취소)
- 기간 필터
- 주문 상세 링크

// API
GET /api/v1/orders?status={}&startDate={}&endDate={}&page={}
```

#### 주문 상세 (/orders/[id])
```typescript
// 기능
- 주문 정보 (주문번호, 일시, 상태)
- 주문 상품 목록
- 배송 정보 (배송지, 배송 상태, 송장번호)
- 결제 정보
- 주문 취소 버튼 (가능한 경우)
- 환불 요청 버튼 (가능한 경우)

// API
GET /api/v1/orders/{id}
POST /api/v1/orders/{id}/cancel
```

---

### 3. 판매자 화면

#### 대시보드 (/seller/dashboard)
```typescript
// 기능
- 오늘 요약 (매출, 주문수, 방문자)
- 주문 현황 (신규, 배송준비, 배송중)
- 매출 차트 (일별/주별/월별)
- 베스트 상품 Top 5
- 최근 주문 목록

// API
GET /api/v1/seller/dashboard/summary
GET /api/v1/seller/dashboard/sales?period={}
GET /api/v1/seller/orders?status=NEW&limit=5
```

#### 상품 관리 (/seller/products)
```typescript
// 기능
- 상품 목록 (테이블)
- 상태 필터 (전체, 판매중, 품절, 숨김)
- 상품 검색
- 일괄 상태 변경
- 상품 등록 버튼

// 테이블 컬럼
- 체크박스
- 이미지
- 상품명
- 카테고리
- 가격
- 재고
- 상태
- 등록일
- 액션 (수정, 삭제)
```

#### 상품 등록/수정 (/seller/products/new, /seller/products/[id]/edit)
```typescript
// 폼 필드
interface ProductForm {
  name: string;
  categoryId: number;
  price: number;
  description: string;
  images: File[];
  options: {
    name: string;
    additionalPrice: number;
    stock: number;
  }[];
  status: 'DRAFT' | 'ON_SALE';
}

// 기능
- 기본 정보 입력
- 카테고리 선택 (드롭다운)
- 이미지 업로드 (드래그앤드롭, 최대 10장)
- 옵션 추가/삭제 (동적 폼)
- 상세 설명 (리치 에디터)
- 임시저장 / 등록

// API
POST /api/v1/seller/products
PUT /api/v1/seller/products/{id}
POST /api/v1/seller/products/{id}/images
```

#### 주문 관리 (/seller/orders)
```typescript
// 기능
- 주문 목록 (테이블)
- 상태 필터 (신규, 결제완료, 배송준비, 배송중, 완료, 취소)
- 기간 필터
- 주문 검색 (주문번호, 구매자)
- 발송 처리 (송장번호 입력)
- 일괄 발송 처리

// API
GET /api/v1/seller/orders?status={}&startDate={}&endDate={}
PUT /api/v1/seller/orders/{id}/ship
```

#### 정산 관리 (/seller/settlements)
```typescript
// 기능
- 정산 내역 목록
- 기간 필터
- 정산 상태 (대기, 확정, 지급완료)
- 정산 상세 (매출, 수수료, 정산금액)

// API
GET /api/v1/seller/settlements?startDate={}&endDate={}
```

---

## 상태 관리

### Zustand Store 정의

#### authStore.ts
```typescript
interface AuthState {
  user: User | null;
  accessToken: string | null;
  isLoading: boolean;
  
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
  checkAuth: () => Promise<void>;
}
```

#### cartStore.ts
```typescript
interface CartState {
  items: CartItem[];
  isLoading: boolean;
  
  fetchCart: () => Promise<void>;
  addItem: (productId: number, optionId: number, quantity: number) => Promise<void>;
  updateQuantity: (itemId: number, quantity: number) => Promise<void>;
  removeItem: (itemId: number) => Promise<void>;
  clearCart: () => void;
  
  // Computed (getter)
  totalAmount: () => number;
  itemCount: () => number;
}
```

### React Query 사용

```typescript
// hooks/useProducts.ts
export function useProducts(filters: ProductFilters) {
  return useQuery({
    queryKey: ['products', filters],
    queryFn: () => productApi.getProducts(filters),
  });
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => productApi.getProduct(id),
  });
}

// 상품 등록 Mutation
export function useCreateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: productApi.createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['seller-products'] });
    },
  });
}
```

---

## API 클라이언트

### 기본 설정
```typescript
// lib/api/client.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor - 토큰 추가
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor - 에러 처리, 토큰 갱신
apiClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    if (error.response?.status === 401) {
      // 토큰 갱신 시도
      await useAuthStore.getState().refreshToken();
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 타입 정의

```typescript
// types/api.ts
interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: {
    code: string;
    message: string;
  } | null;
  timestamp: string;
}

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// types/product.ts
interface Product {
  id: number;
  sellerId: number;
  sellerName: string;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  price: number;
  status: 'DRAFT' | 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN';
  options: ProductOption[];
  images: ProductImage[];
  createdAt: string;
  updatedAt: string;
}

interface ProductOption {
  id: number;
  name: string;
  additionalPrice: number;
  stock: number;
}

// types/order.ts
interface Order {
  id: string;
  buyerId: number;
  items: OrderItem[];
  shippingAddress: ShippingAddress;
  totalAmount: number;
  status: OrderStatus;
  orderedAt: string;
}

type OrderStatus = 
  | 'PENDING' 
  | 'PAID' 
  | 'PREPARING' 
  | 'SHIPPED' 
  | 'DELIVERED' 
  | 'CANCELLED' 
  | 'REFUNDED';
```

---

## 컴포넌트 스펙

### ProductCard
```typescript
interface ProductCardProps {
  product: Product;
  variant?: 'default' | 'horizontal';
  onAddCart?: () => void;
}

// 구성
- 상품 이미지 (hover 시 두 번째 이미지)
- 상품명
- 가격 (할인가 표시)
- 리뷰 평점 및 개수
- 장바구니 아이콘 (hover 시 표시)
```

### ProductOptionSelector
```typescript
interface ProductOptionSelectorProps {
  options: ProductOption[];
  selectedOptionId?: number;
  onChange: (optionId: number) => void;
}

// 기능
- 옵션 목록 표시 (버튼 또는 드롭다운)
- 재고 표시
- 품절 옵션 비활성화
- 추가 금액 표시
```

### OrderStatusBadge
```typescript
interface OrderStatusBadgeProps {
  status: OrderStatus;
}

// 상태별 색상
PENDING: gray
PAID: blue
PREPARING: yellow
SHIPPED: purple
DELIVERED: green
CANCELLED: red
REFUNDED: orange
```

---

## 반응형 브레이크포인트

```typescript
// Tailwind 기본값 사용
sm: 640px   // 모바일 가로
md: 768px   // 태블릿
lg: 1024px  // 데스크탑
xl: 1280px  // 와이드
2xl: 1536px // 울트라와이드
```

---

## 환경변수

```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_PG_CLIENT_KEY=test_client_key
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
```
