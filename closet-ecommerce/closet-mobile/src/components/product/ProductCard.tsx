import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { formatPrice, formatDiscountRate } from '../../utils/format';

interface ProductCardProps {
  id: number;
  name: string;
  brandName: string;
  price: number;
  salePrice?: number;
  discountRate?: number;
  thumbnailUrl: string;
  onPress: (productId: number) => void;
}

const ProductCard: React.FC<ProductCardProps> = ({
  id,
  name,
  brandName,
  price,
  salePrice,
  discountRate,
  onPress,
}) => {
  return (
    <TouchableOpacity style={styles.container} onPress={() => onPress(id)}>
      <View style={styles.image}>
        <Text style={styles.placeholderText}>이미지</Text>
      </View>
      <Text style={styles.brand}>{brandName}</Text>
      <Text style={styles.name} numberOfLines={2}>{name}</Text>
      <View style={styles.priceRow}>
        {discountRate && (
          <Text style={styles.discount}>{formatDiscountRate(discountRate)}</Text>
        )}
        <Text style={styles.price}>{formatPrice(salePrice || price)}</Text>
      </View>
      {salePrice && (
        <Text style={styles.originalPrice}>{formatPrice(price)}</Text>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 8,
  },
  image: {
    height: 200,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    color: '#999',
    fontSize: 12,
  },
  brand: {
    fontSize: 11,
    color: '#999',
    marginTop: 8,
  },
  name: {
    fontSize: 13,
    color: '#333',
    marginTop: 2,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 4,
  },
  discount: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#e74c3c',
  },
  price: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  originalPrice: {
    fontSize: 12,
    color: '#999',
    textDecorationLine: 'line-through',
  },
});

export default ProductCard;
