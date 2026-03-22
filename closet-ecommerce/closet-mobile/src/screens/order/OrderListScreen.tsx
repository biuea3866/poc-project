import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { RootNavigationProp } from '../../navigation/types';
import { formatPrice, formatDate, formatOrderStatus } from '../../utils/format';

const MOCK_ORDERS = [
  { id: 1, orderNumber: 'ORD-20260301-001', status: 'DELIVERED', totalPrice: 59000, orderedAt: '2026-03-01T10:00:00' },
  { id: 2, orderNumber: 'ORD-20260310-002', status: 'SHIPPING', totalPrice: 128000, orderedAt: '2026-03-10T14:30:00' },
  { id: 3, orderNumber: 'ORD-20260315-003', status: 'PREPARING', totalPrice: 45000, orderedAt: '2026-03-15T09:00:00' },
];

const OrderListScreen: React.FC = () => {
  const navigation = useNavigation<RootNavigationProp>();

  return (
    <View style={styles.container}>
      <FlatList
        data={MOCK_ORDERS}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.orderCard}
            onPress={() => navigation.navigate('OrderDetail', { orderId: item.id })}
          >
            <View style={styles.orderHeader}>
              <Text style={styles.orderNumber}>{item.orderNumber}</Text>
              <Text style={styles.orderDate}>{formatDate(item.orderedAt)}</Text>
            </View>
            <View style={styles.orderBody}>
              <View style={styles.orderImage}>
                <Text style={styles.placeholderText}>이미지</Text>
              </View>
              <View style={styles.orderInfo}>
                <Text style={styles.statusBadge}>{formatOrderStatus(item.status)}</Text>
                <Text style={styles.orderPrice}>{formatPrice(item.totalPrice)}</Text>
              </View>
            </View>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>주문 내역이 없습니다</Text>
          </View>
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  orderCard: {
    backgroundColor: '#fff',
    marginBottom: 8,
    padding: 16,
  },
  orderHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  orderNumber: {
    fontSize: 13,
    fontWeight: '600',
  },
  orderDate: {
    fontSize: 12,
    color: '#999',
  },
  orderBody: {
    flexDirection: 'row',
  },
  orderImage: {
    width: 60,
    height: 60,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    color: '#999',
    fontSize: 10,
  },
  orderInfo: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'center',
  },
  statusBadge: {
    fontSize: 13,
    fontWeight: 'bold',
    color: '#333',
  },
  orderPrice: {
    fontSize: 15,
    fontWeight: 'bold',
    marginTop: 4,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 80,
  },
  emptyText: {
    fontSize: 14,
    color: '#999',
  },
});

export default OrderListScreen;
