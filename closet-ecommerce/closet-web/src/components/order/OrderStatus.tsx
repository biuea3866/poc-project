interface OrderStatusProps {
  status: string;
}

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: '결제대기', color: 'bg-yellow-100 text-yellow-800' },
  PAID: { label: '결제완료', color: 'bg-blue-100 text-blue-800' },
  PREPARING: { label: '상품준비중', color: 'bg-indigo-100 text-indigo-800' },
  SHIPPING: { label: '배송중', color: 'bg-purple-100 text-purple-800' },
  DELIVERED: { label: '배송완료', color: 'bg-green-100 text-green-800' },
  CANCELLED: { label: '취소됨', color: 'bg-gray-100 text-gray-800' },
  REFUNDED: { label: '환불완료', color: 'bg-red-100 text-red-800' },
};

export default function OrderStatusBadge({ status }: OrderStatusProps) {
  const config = statusConfig[status] || { label: status, color: 'bg-gray-100 text-gray-800' };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
}
