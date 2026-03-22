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
import { useCart } from '../../hooks/useCart';
import { formatPrice } from '../../utils/format';

const CartScreen: React.FC = () => {
  const navigation = useNavigation<RootNavigationProp>();
  const { items, totalPrice, deliveryFee, finalPrice, removeItem, updateItemQuantity } = useCart();

  if (items.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>장바구니가 비어있습니다</Text>
        <TouchableOpacity
          style={styles.shopButton}
          onPress={() => navigation.navigate('Main')}
        >
          <Text style={styles.shopButtonText}>쇼핑하러 가기</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={items}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <View style={styles.cartItem}>
            <View style={styles.itemImage}>
              <Text style={styles.placeholderText}>이미지</Text>
            </View>
            <View style={styles.itemInfo}>
              <Text style={styles.brandName}>{item.brandName}</Text>
              <Text style={styles.itemName} numberOfLines={2}>
                {item.productName}
              </Text>
              <Text style={styles.optionText}>
                {item.color} / {item.size}
              </Text>
              <Text style={styles.itemPrice}>
                {formatPrice(item.salePrice || item.price)}
              </Text>
              <View style={styles.quantityRow}>
                <TouchableOpacity
                  style={styles.quantityButton}
                  onPress={() => updateItemQuantity(item.id, Math.max(1, item.quantity - 1))}
                >
                  <Text style={styles.quantityButtonText}>-</Text>
                </TouchableOpacity>
                <Text style={styles.quantity}>{item.quantity}</Text>
                <TouchableOpacity
                  style={styles.quantityButton}
                  onPress={() => updateItemQuantity(item.id, item.quantity + 1)}
                >
                  <Text style={styles.quantityButtonText}>+</Text>
                </TouchableOpacity>
                <TouchableOpacity onPress={() => removeItem(item.id)}>
                  <Text style={styles.removeText}>삭제</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}
        ListFooterComponent={
          <View style={styles.summary}>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>상품 금액</Text>
              <Text style={styles.summaryValue}>{formatPrice(totalPrice)}</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>배송비</Text>
              <Text style={styles.summaryValue}>
                {deliveryFee === 0 ? '무료' : formatPrice(deliveryFee)}
              </Text>
            </View>
            <View style={[styles.summaryRow, styles.totalRow]}>
              <Text style={styles.totalLabel}>결제 예상 금액</Text>
              <Text style={styles.totalValue}>{formatPrice(finalPrice)}</Text>
            </View>
          </View>
        }
      />

      <View style={styles.bottomBar}>
        <TouchableOpacity
          style={styles.checkoutButton}
          onPress={() =>
            navigation.navigate('Checkout', {
              cartItemIds: items.map((item) => item.id),
            })
          }
        >
          <Text style={styles.checkoutButtonText}>
            주문하기 ({items.length}개)
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginBottom: 16,
  },
  shopButton: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: '#000',
    borderRadius: 8,
  },
  shopButtonText: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  cartItem: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  itemImage: {
    width: 80,
    height: 100,
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
  optionText: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  itemPrice: {
    fontSize: 15,
    fontWeight: 'bold',
    marginTop: 4,
  },
  quantityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 8,
  },
  quantityButton: {
    width: 28,
    height: 28,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  quantityButtonText: {
    fontSize: 16,
  },
  quantity: {
    fontSize: 14,
    minWidth: 20,
    textAlign: 'center',
  },
  removeText: {
    fontSize: 12,
    color: '#999',
    marginLeft: 8,
  },
  summary: {
    padding: 16,
    borderTopWidth: 8,
    borderTopColor: '#f5f5f5',
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
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
    paddingTop: 16,
  },
  totalLabel: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  totalValue: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  bottomBar: {
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  checkoutButton: {
    backgroundColor: '#000',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
  },
  checkoutButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default CartScreen;
