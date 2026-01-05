-- Payments table
CREATE TABLE IF NOT EXISTS payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  provider TEXT NOT NULL,
  paymentKey TEXT NOT NULL UNIQUE,
  orderId TEXT NOT NULL,
  orderName TEXT NOT NULL,
  amount INTEGER NOT NULL,
  status TEXT NOT NULL,
  method TEXT,
  cardCompany TEXT,
  cardNumber TEXT,
  approveNo TEXT,
  balanceAmount INTEGER NOT NULL,
  successUrl TEXT NOT NULL,
  failUrl TEXT NOT NULL,
  customerEmail TEXT,
  customerName TEXT,
  customerPhone TEXT,
  createdAt TEXT NOT NULL,
  approvedAt TEXT
);

-- Payment cancels table
CREATE TABLE IF NOT EXISTS payment_cancels (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  paymentId INTEGER NOT NULL,
  transactionKey TEXT NOT NULL,
  cancelReason TEXT NOT NULL,
  cancelAmount INTEGER NOT NULL,
  canceledAt TEXT NOT NULL,
  FOREIGN KEY (paymentId) REFERENCES payments(id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_payments_orderId ON payments(orderId);
CREATE INDEX IF NOT EXISTS idx_payments_provider ON payments(provider);
CREATE INDEX IF NOT EXISTS idx_payment_cancels_paymentId ON payment_cancels(paymentId);
