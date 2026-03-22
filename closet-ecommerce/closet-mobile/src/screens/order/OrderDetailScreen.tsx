import React from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
} from 'react-native';
import { useRoute } from '@react-navigation/native';
import { OrderDetailRouteProp } from '../../navigation/types';
import { formatPrice, formatDateTime, formatOrderStatus } from '../../utils/format';

const OrderDetailScreen: React.FC = () => {
  const route = useRoute<OrderDetailRouteProp>();
  const { orderId } = route.params;

  return (
    <ScrollView style={styles.container}>
      {/* Order Status */}
      <View style={styles.section}>
        <Text style={styles.statusLabel}>주문 상태</Text>
        <Text style={styles.statusValue}>{formatOrderStatus('SHIPPING')}</Text>
        <Text style={styles.orderNumber}>주문번호: ORD-20260310-{orderId.toString().padStart(3, '0')}</Text>
        <Text style={styles.orderDate}>주문일시: {formatDateTime('2026-03-10T14:30:00')}</Text>
      </View>

      {/* Order Items */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>주문 상품</Text>
        {[1, 2].map((itemId) => (
          <View key={itemId} style={styles.orderItem}>
            <View style={styles.itemImage}>
              <Text style={styles.placeholderText}>이미지</Text>
            </View>
            <View style={styles.itemInfo}>
              <Text style={styles.brandName}>브랜드명</Text>
              <Text style={styles.itemName}>상품명 #{itemId}</Text>
              <Text style={styles.itemOption}>블랙 / M</Text>
              <Text style={styles.itemPrice}>{formatPrice(59000)} / 1개</Text>
            </View>
          </View>
        ))}
      </View>

      {/* Shipping Info */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>배송 정보</Text>
        <Text style={styles.infoText}>수령인: 홍길동</Text>
        <Text style={styles.infoText}>연락처: 010-1234-5678</Text>
        <Text style={styles.infoText}>주소: 서울시 강남구 역삼동 123-45</Text>
      </View>

      {/* Payment Info */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>결제 정보</Text>
        <View style={styles.summaryRow}>
          <Text style={styles.summaryLabel}>상품 금액</Text>
          <Text style={styles.summaryValue}>{formatPrice(118000)}</Text>
        </View>
        <View style={styles.summaryRow}>
          <Text style={styles.summaryLabel}>배송비</Text>
          <Text style={styles.summaryValue}>무료</Text>
        </View>
        <View style={[styles.summaryRow, styles.totalRow]}>
          <Text style={styles.totalLabel}>총 결제 금액</Text>
          <Text style={styles.totalValue}>{formatPrice(118000)}</Text>
        </View>
        <Text style={styles.paymentMethod}>결제수단: 신용카드</Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  section: {
    backgroundColor: '#fff',
    padding: 16,
    marginBottom: 8,
  },
  statusLabel: {
    fontSize: 13,
    color: '#666',
  },
  statusValue: {
    fontSize: 20,
    fontWeight: 'bold',
    marginTop: 4,
  },
  orderNumber: {
    fontSize: 12,
    color: '#999',
    marginTop: 8,
  },
  orderDate: {
    fontSize: 12,
    color: '#999',
    marginTop: 2,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  orderItem: {
    flexDirection: 'row',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  itemImage: {
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
  itemInfo: {
    flex: 1,
    marginLeft: 12,
  },
  brandName: {
    fontSize: 11,
    color: '#999',
  },
  itemName: {
    fontSize: 13,
    color: '#333',
    marginTop: 2,
  },
  itemOption: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  itemPrice: {
    fontSize: 14,
    fontWeight: 'bold',
    marginTop: 4,
  },
  infoText: {
    fontSize: 14,
    color: '#333',
    lineHeight: 22,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
  },
  summaryLabel: {
    fontSize: 14,
    color: '#666',
  },
  summaryValue: {
    fontSize: 14,
    color: '#333',
  },
  totalRow: {
    borderTopWidth: 1,
    borderTopColor: '#eee',
    marginTop: 8,
    paddingTop: 12,
  },
  totalLabel: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  totalValue: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  paymentMethod: {
    fontSize: 13,
    color: '#666',
    marginTop: 12,
  },
});

export default OrderDetailScreen;
