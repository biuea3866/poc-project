// ==================== Common Types ====================
export enum PaymentStatus {
  READY = 'READY',
  DONE = 'DONE',
  CANCELED = 'CANCELED',
  PARTIAL_CANCELED = 'PARTIAL_CANCELED',
  FAILED = 'FAILED',
}

export enum PgProvider {
  TOSS_PAYMENTS = 'TOSS_PAYMENTS',
  KAKAO_PAY = 'KAKAO_PAY',
  NAVER_PAY = 'NAVER_PAY',
  DANAL = 'DANAL',
}

// ==================== Database Models ====================
export interface Payment {
  id: number;
  provider: PgProvider;
  paymentKey: string;
  orderId: string;
  orderName: string;
  amount: number;
  status: PaymentStatus;
  method?: string;
  cardCompany?: string;
  cardNumber?: string;
  approveNo?: string;
  balanceAmount: number;
  successUrl: string;
  failUrl: string;
  customerEmail?: string;
  customerName?: string;
  customerPhone?: string;
  createdAt: string;
  approvedAt?: string;
}

export interface PaymentCancel {
  id: number;
  paymentId: number;
  transactionKey: string;
  cancelReason: string;
  cancelAmount: number;
  canceledAt: string;
}

// ==================== Toss Payments ====================
export interface TossPaymentRequest {
  amount: number;
  orderId: string;
  orderName: string;
  successUrl: string;
  failUrl: string;
  customerEmail?: string;
  customerName?: string;
  customerMobilePhone?: string;
}

export interface TossPaymentResponse {
  paymentKey: string;
  orderId: string;
  status: string;
  requestedAt: string;
  checkout: {
    url: string;
  };
}

export interface TossConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export interface TossConfirmResponse {
  paymentKey: string;
  orderId: string;
  status: string;
  totalAmount: number;
  balanceAmount: number;
  method: string;
  approvedAt: string;
  card?: {
    company: string;
    number: string;
    installmentPlanMonths: number;
    isInterestFree: boolean;
    approveNo: string;
  };
  receipt: {
    url: string;
  };
}

export interface TossCancelRequest {
  cancelReason: string;
  cancelAmount?: number;
}

// ==================== Kakao Pay ====================
export interface KakaoPayReadyRequest {
  cid: string;
  partner_order_id: string;
  partner_user_id: string;
  item_name: string;
  quantity: number;
  total_amount: number;
  tax_free_amount: number;
  approval_url: string;
  cancel_url: string;
  fail_url: string;
}

export interface KakaoPayReadyResponse {
  tid: string;
  next_redirect_app_url: string;
  next_redirect_mobile_url: string;
  next_redirect_pc_url: string;
  android_app_scheme: string;
  ios_app_scheme: string;
  created_at: string;
}

export interface KakaoPayApproveRequest {
  cid: string;
  tid: string;
  partner_order_id: string;
  partner_user_id: string;
  pg_token: string;
}

export interface KakaoPayApproveResponse {
  aid: string;
  tid: string;
  cid: string;
  partner_order_id: string;
  partner_user_id: string;
  payment_method_type: string;
  item_name: string;
  quantity: number;
  amount: {
    total: number;
    tax_free: number;
    vat: number;
    point: number;
    discount: number;
  };
  card_info?: {
    purchase_corp: string;
    purchase_corp_code: string;
    issuer_corp: string;
    issuer_corp_code: string;
    bin: string;
    card_type: string;
    install_month: string;
    approved_id: string;
    card_mid: string;
  };
  created_at: string;
  approved_at: string;
}

// ==================== Naver Pay ====================
export interface NaverPayReserveRequest {
  merchantPayKey: string;
  productName: string;
  productCount: number;
  totalPayAmount: number;
  taxScopeAmount: number;
  taxExScopeAmount: number;
  returnUrl: string;
  merchantUserKey: string;
}

export interface NaverPayReserveResponse {
  code: string;
  message: string;
  body: {
    reserveId: string;
    paymentUrl: string;
  };
}

export interface NaverPayApproveResponse {
  code: string;
  message: string;
  body: {
    paymentId: string;
    merchantPayKey: string;
    merchantUserKey: string;
    paymentResult: {
      paymentMethod: string;
      totalPayAmount: number;
      cardCorpName: string;
      cardNo: string;
      admissionYmdt: string;
    };
    detail: {
      productName: string;
      productCount: number;
    };
  };
}

// ==================== Danal ====================
export interface DanalReadyRequest {
  amount: string;
  orderNo: string;
  itemName: string;
  userName: string;
  userPhone: string;
  returnUrl: string;
  cancelUrl: string;
}

export interface DanalReadyResponse {
  result: string;
  message: string;
  data: {
    tid: string;
    paymentUrl: string;
  };
}

export interface DanalConfirmRequest {
  tid: string;
  orderNo: string;
  amount: string;
}

export interface DanalConfirmResponse {
  result: string;
  message: string;
  data: {
    tid: string;
    orderNo: string;
    amount: string;
    payMethod: string;
    cardName: string;
    cardNo: string;
    installMonth: string;
    authNo: string;
    transDate: string;
  };
}

// ==================== Scenarios ====================
export interface ScenarioConfig {
  status?: number;
  delay?: number;
  body?: any;
}

export type ScenarioType =
  | 'success'
  | 'error-card-declined'
  | 'error-insufficient-balance'
  | 'error-invalid-card'
  | 'error-expired-card'
  | 'timeout';
