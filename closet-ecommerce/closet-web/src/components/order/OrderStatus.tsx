import { OrderStatus as OrderStatusEnum } from '@/types/order';

interface OrderStatusProps {
  status: OrderStatusEnum;
}

const statusConfig: Record<OrderStatusEnum, { label: string; color: string }> = {
  [OrderStatusEnum.PENDING]: { label: 'Pending', color: 'bg-yellow-100 text-yellow-800' },
  [OrderStatusEnum.PAID]: { label: 'Paid', color: 'bg-blue-100 text-blue-800' },
  [OrderStatusEnum.PREPARING]: { label: 'Preparing', color: 'bg-indigo-100 text-indigo-800' },
  [OrderStatusEnum.SHIPPING]: { label: 'Shipping', color: 'bg-purple-100 text-purple-800' },
  [OrderStatusEnum.DELIVERED]: { label: 'Delivered', color: 'bg-green-100 text-green-800' },
  [OrderStatusEnum.CANCELLED]: { label: 'Cancelled', color: 'bg-gray-100 text-gray-800' },
  [OrderStatusEnum.REFUNDED]: { label: 'Refunded', color: 'bg-red-100 text-red-800' },
};

export default function OrderStatusBadge({ status }: OrderStatusProps) {
  const config = statusConfig[status];

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
}
