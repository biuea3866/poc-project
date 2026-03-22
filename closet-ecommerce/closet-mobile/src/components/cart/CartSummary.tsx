import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { formatPrice } from '../../utils/format';

interface CartSummaryProps {
  totalPrice: number;
  discountPrice: number;
  deliveryFee: number;
  finalPrice: number;
}

const CartSummary: React.FC<CartSummaryProps> = ({
  totalPrice,
  discountPrice,
  deliveryFee,
  finalPrice,
}) => {
  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Text style={styles.label}>상품 금액</Text>
        <Text style={styles.value}>{formatPrice(totalPrice)}</Text>
      </View>
      {discountPrice > 0 && (
        <View style={styles.row}>
          <Text style={styles.label}>할인 금액</Text>
          <Text style={[styles.value, styles.discount]}>-{formatPrice(discountPrice)}</Text>
        </View>
      )}
      <View style={styles.row}>
        <Text style={styles.label}>배송비</Text>
        <Text style={styles.value}>{deliveryFee === 0 ? '무료' : formatPrice(deliveryFee)}</Text>
      </View>
      <View style={[styles.row, styles.totalRow]}>
        <Text style={styles.totalLabel}>결제 예상 금액</Text>
        <Text style={styles.totalValue}>{formatPrice(finalPrice)}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    borderTopWidth: 8,
    borderTopColor: '#f5f5f5',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
  },
  label: { fontSize: 14, color: '#666' },
  value: { fontSize: 14, color: '#333' },
  discount: { color: '#e74c3c' },
  totalRow: {
    borderTopWidth: 1,
    borderTopColor: '#eee',
    marginTop: 8,
    paddingTop: 12,
  },
  totalLabel: { fontSize: 16, fontWeight: 'bold' },
  totalValue: { fontSize: 18, fontWeight: 'bold' },
});

export default CartSummary;
