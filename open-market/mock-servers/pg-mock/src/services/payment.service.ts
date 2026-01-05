import { v4 as uuidv4 } from 'uuid';
import { db } from '../db';
import { Payment, PaymentCancel, PaymentStatus, PgProvider } from '../types';

export class PaymentService {
  async createPayment(data: {
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
  }): Promise<Payment> {
    const now = new Date().toISOString();

    const result = await db.run(
      `INSERT INTO payments (
        provider, paymentKey, orderId, orderName, amount, status,
        balanceAmount, successUrl, failUrl, customerEmail, customerName,
        customerPhone, createdAt
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        data.provider,
        data.paymentKey,
        data.orderId,
        data.orderName,
        data.amount,
        PaymentStatus.READY,
        data.amount,
        data.successUrl,
        data.failUrl,
        data.customerEmail || null,
        data.customerName || null,
        data.customerPhone || null,
        now,
      ]
    );

    const payment = await this.getPaymentById(result.lastID);
    if (!payment) {
      throw new Error('Failed to create payment');
    }

    return payment;
  }

  async getPaymentById(id: number): Promise<Payment | undefined> {
    return db.get<Payment>('SELECT * FROM payments WHERE id = ?', [id]);
  }

  async getPaymentByKey(paymentKey: string): Promise<Payment | undefined> {
    return db.get<Payment>('SELECT * FROM payments WHERE paymentKey = ?', [paymentKey]);
  }

  async getPaymentByOrderId(orderId: string): Promise<Payment | undefined> {
    return db.get<Payment>('SELECT * FROM payments WHERE orderId = ?', [orderId]);
  }

  async approvePayment(paymentKey: string): Promise<Payment> {
    const payment = await this.getPaymentByKey(paymentKey);
    if (!payment) {
      throw new Error('Payment not found');
    }

    if (payment.status !== PaymentStatus.READY) {
      throw new Error('Payment is not in READY status');
    }

    const now = new Date().toISOString();

    await db.run(
      `UPDATE payments SET
        status = ?,
        method = ?,
        cardCompany = ?,
        cardNumber = ?,
        approveNo = ?,
        approvedAt = ?
      WHERE paymentKey = ?`,
      [
        PaymentStatus.DONE,
        '카드',
        '신한',
        '4321-****-****-1234',
        Math.floor(Math.random() * 90000000) + 10000000, // Random 8-digit number
        now,
        paymentKey,
      ]
    );

    const updatedPayment = await this.getPaymentByKey(paymentKey);
    if (!updatedPayment) {
      throw new Error('Failed to approve payment');
    }

    return updatedPayment;
  }

  async cancelPayment(
    paymentKey: string,
    cancelReason: string,
    cancelAmount?: number
  ): Promise<{ payment: Payment; cancel: PaymentCancel }> {
    const payment = await this.getPaymentByKey(paymentKey);
    if (!payment) {
      throw new Error('Payment not found');
    }

    if (payment.status !== PaymentStatus.DONE && payment.status !== PaymentStatus.PARTIAL_CANCELED) {
      throw new Error('Payment cannot be canceled');
    }

    const amountToCancel = cancelAmount || payment.balanceAmount;

    if (amountToCancel > payment.balanceAmount) {
      throw new Error('Cancel amount exceeds balance amount');
    }

    const newBalanceAmount = payment.balanceAmount - amountToCancel;
    const newStatus =
      newBalanceAmount === 0 ? PaymentStatus.CANCELED : PaymentStatus.PARTIAL_CANCELED;

    const now = new Date().toISOString();
    const transactionKey = `cancel_txn_${uuidv4()}`;

    // Update payment
    await db.run(
      'UPDATE payments SET status = ?, balanceAmount = ? WHERE paymentKey = ?',
      [newStatus, newBalanceAmount, paymentKey]
    );

    // Insert cancel record
    const cancelResult = await db.run(
      `INSERT INTO payment_cancels (
        paymentId, transactionKey, cancelReason, cancelAmount, canceledAt
      ) VALUES (?, ?, ?, ?, ?)`,
      [payment.id, transactionKey, cancelReason, amountToCancel, now]
    );

    const updatedPayment = await this.getPaymentByKey(paymentKey);
    const cancel = await db.get<PaymentCancel>(
      'SELECT * FROM payment_cancels WHERE id = ?',
      [cancelResult.lastID]
    );

    if (!updatedPayment || !cancel) {
      throw new Error('Failed to cancel payment');
    }

    return { payment: updatedPayment, cancel };
  }

  async getCancelsByPaymentId(paymentId: number): Promise<PaymentCancel[]> {
    return db.all<PaymentCancel>(
      'SELECT * FROM payment_cancels WHERE paymentId = ? ORDER BY canceledAt DESC',
      [paymentId]
    );
  }
}

export const paymentService = new PaymentService();
