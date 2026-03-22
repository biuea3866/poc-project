import React from 'react';
import { OrderStatus } from '../../types/order';
import { formatOrderStatus } from '../../utils/format';
import Badge from '../common/Badge';

interface OrderStatusBadgeProps {
  status: OrderStatus;
}

const statusVariantMap: Record<OrderStatus, 'default' | 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'default',
  PAID: 'info',
  PREPARING: 'info',
  SHIPPING: 'warning',
  DELIVERED: 'success',
  CANCELLED: 'danger',
  RETURN_REQUESTED: 'warning',
  RETURNED: 'danger',
  EXCHANGE_REQUESTED: 'warning',
  EXCHANGED: 'info',
};

const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({ status }) => {
  return (
    <Badge
      text={formatOrderStatus(status)}
      variant={statusVariantMap[status]}
    />
  );
};

export default OrderStatusBadge;
