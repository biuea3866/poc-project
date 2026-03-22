import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { OrderItem } from '../../types/order';
import { formatPrice } from '../../utils/format';
import OrderStatusBadge from './OrderStatusBadge';

interface OrderItemRowProps {
  item: OrderItem;
}

const OrderItemRow: React.FC<OrderItemRowProps> = ({ item }) => {
  return (
    <View style={styles.container}>
      <View style={styles.image}>
        <Text style={styles.placeholderText}>이미지</Text>
      </View>
      <View style={styles.info}>
        <Text style={styles.brand}>{item.brandName}</Text>
        <Text style={styles.name} numberOfLines={2}>{item.productName}</Text>
        <Text style={styles.option}>{item.color} / {item.size}</Text>
        <Text style={styles.price}>{formatPrice(item.price)} / {item.quantity}개</Text>
        <OrderStatusBadge status={item.status} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  image: {
    width: 60,
    height: 60,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: { color: '#999', fontSize: 10 },
  info: { flex: 1, marginLeft: 12 },
  brand: { fontSize: 11, color: '#999' },
  name: { fontSize: 13, color: '#333', marginTop: 2 },
  option: { fontSize: 12, color: '#666', marginTop: 2 },
  price: { fontSize: 14, fontWeight: 'bold', marginTop: 4, marginBottom: 4 },
});

export default OrderItemRow;
