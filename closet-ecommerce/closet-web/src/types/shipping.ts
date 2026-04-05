export type ShippingStatus = 'READY' | 'PICKED_UP' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

export interface ShippingTracking {
  shippingId: number;
  orderId: number;
  trackingNumber: string;
  carrier: string;
  status: ShippingStatus;
  estimatedDeliveryDate: string | null;
  deliveredAt: string | null;
  logs: TrackingLog[];
}

export interface TrackingLog {
  id: number;
  status: ShippingStatus;
  location: string;
  description: string;
  timestamp: string;
}
