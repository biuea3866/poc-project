import { Payment, PaymentCancel, PgProvider } from '../types';
export declare class PaymentService {
    createPayment(data: {
        provider: PgProvider;
        paymentKey: string;
        orderId: string;
        orderName: string;
        amount: number;
        successUrl: string;
        failUrl: string;
        customerEmail?: string;
        customerName?: string;
        customerPhone?: string;
    }): Promise<Payment>;
    getPaymentById(id: number): Promise<Payment | undefined>;
    getPaymentByKey(paymentKey: string): Promise<Payment | undefined>;
    getPaymentByOrderId(orderId: string): Promise<Payment | undefined>;
    approvePayment(paymentKey: string): Promise<Payment>;
    cancelPayment(paymentKey: string, cancelReason: string, cancelAmount?: number): Promise<{
        payment: Payment;
        cancel: PaymentCancel;
    }>;
    getCancelsByPaymentId(paymentId: number): Promise<PaymentCancel[]>;
}
export declare const paymentService: PaymentService;
//# sourceMappingURL=payment.service.d.ts.map