"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.paymentService = exports.PaymentService = void 0;
const uuid_1 = require("uuid");
const db_1 = require("../db");
const types_1 = require("../types");
class PaymentService {
    async createPayment(data) {
        const now = new Date().toISOString();
        const result = await db_1.db.run(`INSERT INTO payments (
        provider, paymentKey, orderId, orderName, amount, status,
        balanceAmount, successUrl, failUrl, customerEmail, customerName,
        customerPhone, createdAt
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`, [
            data.provider,
            data.paymentKey,
            data.orderId,
            data.orderName,
            data.amount,
            types_1.PaymentStatus.READY,
            data.amount,
            data.successUrl,
            data.failUrl,
            data.customerEmail || null,
            data.customerName || null,
            data.customerPhone || null,
            now,
        ]);
        const payment = await this.getPaymentById(result.lastID);
        if (!payment) {
            throw new Error('Failed to create payment');
        }
        return payment;
    }
    async getPaymentById(id) {
        return db_1.db.get('SELECT * FROM payments WHERE id = ?', [id]);
    }
    async getPaymentByKey(paymentKey) {
        return db_1.db.get('SELECT * FROM payments WHERE paymentKey = ?', [paymentKey]);
    }
    async getPaymentByOrderId(orderId) {
        return db_1.db.get('SELECT * FROM payments WHERE orderId = ?', [orderId]);
    }
    async approvePayment(paymentKey) {
        const payment = await this.getPaymentByKey(paymentKey);
        if (!payment) {
            throw new Error('Payment not found');
        }
        if (payment.status !== types_1.PaymentStatus.READY) {
            throw new Error('Payment is not in READY status');
        }
        const now = new Date().toISOString();
        await db_1.db.run(`UPDATE payments SET
        status = ?,
        method = ?,
        cardCompany = ?,
        cardNumber = ?,
        approveNo = ?,
        approvedAt = ?
      WHERE paymentKey = ?`, [
            types_1.PaymentStatus.DONE,
            '카드',
            '신한',
            '4321-****-****-1234',
            Math.floor(Math.random() * 90000000) + 10000000, // Random 8-digit number
            now,
            paymentKey,
        ]);
        const updatedPayment = await this.getPaymentByKey(paymentKey);
        if (!updatedPayment) {
            throw new Error('Failed to approve payment');
        }
        return updatedPayment;
    }
    async cancelPayment(paymentKey, cancelReason, cancelAmount) {
        const payment = await this.getPaymentByKey(paymentKey);
        if (!payment) {
            throw new Error('Payment not found');
        }
        if (payment.status !== types_1.PaymentStatus.DONE && payment.status !== types_1.PaymentStatus.PARTIAL_CANCELED) {
            throw new Error('Payment cannot be canceled');
        }
        const amountToCancel = cancelAmount || payment.balanceAmount;
        if (amountToCancel > payment.balanceAmount) {
            throw new Error('Cancel amount exceeds balance amount');
        }
        const newBalanceAmount = payment.balanceAmount - amountToCancel;
        const newStatus = newBalanceAmount === 0 ? types_1.PaymentStatus.CANCELED : types_1.PaymentStatus.PARTIAL_CANCELED;
        const now = new Date().toISOString();
        const transactionKey = `cancel_txn_${(0, uuid_1.v4)()}`;
        // Update payment
        await db_1.db.run('UPDATE payments SET status = ?, balanceAmount = ? WHERE paymentKey = ?', [newStatus, newBalanceAmount, paymentKey]);
        // Insert cancel record
        const cancelResult = await db_1.db.run(`INSERT INTO payment_cancels (
        paymentId, transactionKey, cancelReason, cancelAmount, canceledAt
      ) VALUES (?, ?, ?, ?, ?)`, [payment.id, transactionKey, cancelReason, amountToCancel, now]);
        const updatedPayment = await this.getPaymentByKey(paymentKey);
        const cancel = await db_1.db.get('SELECT * FROM payment_cancels WHERE id = ?', [cancelResult.lastID]);
        if (!updatedPayment || !cancel) {
            throw new Error('Failed to cancel payment');
        }
        return { payment: updatedPayment, cancel };
    }
    async getCancelsByPaymentId(paymentId) {
        return db_1.db.all('SELECT * FROM payment_cancels WHERE paymentId = ? ORDER BY canceledAt DESC', [paymentId]);
    }
}
exports.PaymentService = PaymentService;
exports.paymentService = new PaymentService();
//# sourceMappingURL=payment.service.js.map