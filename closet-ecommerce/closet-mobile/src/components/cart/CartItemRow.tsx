import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { CartItem } from '../../types/cart';
import { formatPrice } from '../../utils/format';

interface CartItemRowProps {
  item: CartItem;
  onUpdateQuantity: (cartItemId: number, quantity: number) => void;
  onRemove: (cartItemId: number) => void;
}

const CartItemRow: React.FC<CartItemRowProps> = ({ item, onUpdateQuantity, onRemove }) => {
  return (
    <View style={styles.container}>
      <View style={styles.image}>
        <Text style={styles.placeholderText}>이미지</Text>
      </View>
      <View style={styles.info}>
        <Text style={styles.brand}>{item.brandName}</Text>
        <Text style={styles.name} numberOfLines={2}>{item.productName}</Text>
        <Text style={styles.option}>{item.color} / {item.size}</Text>
        <Text style={styles.price}>{formatPrice(item.salePrice || item.price)}</Text>
        <View style={styles.quantityRow}>
          <TouchableOpacity
            style={styles.qtyBtn}
            onPress={() => onUpdateQuantity(item.id, Math.max(1, item.quantity - 1))}
          >
            <Text>-</Text>
          </TouchableOpacity>
          <Text style={styles.qty}>{item.quantity}</Text>
          <TouchableOpacity
            style={styles.qtyBtn}
            onPress={() => onUpdateQuantity(item.id, item.quantity + 1)}
          >
            <Text>+</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => onRemove(item.id)}>
            <Text style={styles.removeText}>삭제</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  image: {
    width: 80,
    height: 100,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: { color: '#999', fontSize: 10 },
  info: { flex: 1, marginLeft: 12 },
  brand: { fontSize: 11, color: '#999' },
  name: { fontSize: 13, color: '#333', marginTop: 2 },
  option: { fontSize: 12, color: '#666', marginTop: 4 },
  price: { fontSize: 15, fontWeight: 'bold', marginTop: 4 },
  quantityRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 8 },
  qtyBtn: {
    width: 28, height: 28, borderWidth: 1, borderColor: '#ddd',
    borderRadius: 4, justifyContent: 'center', alignItems: 'center',
  },
  qty: { fontSize: 14, minWidth: 20, textAlign: 'center' },
  removeText: { fontSize: 12, color: '#999', marginLeft: 8 },
});

export default CartItemRow;
