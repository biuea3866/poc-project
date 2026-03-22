import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Alert,
} from 'react-native';
import { useRoute } from '@react-navigation/native';
import { ProductDetailRouteProp } from '../../navigation/types';
import { useCart } from '../../hooks/useCart';
import { formatPrice } from '../../utils/format';

const ProductDetailScreen: React.FC = () => {
  const route = useRoute<ProductDetailRouteProp>();
  const { productId } = route.params;
  const { addItem } = useCart();
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);

  const sizes = ['S', 'M', 'L', 'XL'];
  const colors = ['블랙', '화이트', '네이비', '그레이'];

  const handleAddToCart = async () => {
    if (!selectedSize || !selectedColor) {
      Alert.alert('알림', '사이즈와 색상을 선택해주세요.');
      return;
    }
    // In real app, optionId would be determined by size+color
    await addItem(productId, 1, 1);
    Alert.alert('장바구니', '장바구니에 추가되었습니다.');
  };

  return (
    <View style={styles.container}>
      <ScrollView>
        {/* Product Image */}
        <View style={styles.imageContainer}>
          <Text style={styles.placeholderText}>상품 이미지 #{productId}</Text>
        </View>

        {/* Product Info */}
        <View style={styles.infoSection}>
          <Text style={styles.brandName}>브랜드명</Text>
          <Text style={styles.productName}>상품명 placeholder #{productId}</Text>
          <View style={styles.priceRow}>
            <Text style={styles.salePrice}>{formatPrice(59000)}</Text>
            <Text style={styles.originalPrice}>{formatPrice(79000)}</Text>
            <Text style={styles.discount}>25%</Text>
          </View>
          <View style={styles.ratingRow}>
            <Text style={styles.rating}>4.5</Text>
            <Text style={styles.reviewCount}>리뷰 128개</Text>
          </View>
        </View>

        {/* Size Selection */}
        <View style={styles.optionSection}>
          <Text style={styles.optionTitle}>사이즈</Text>
          <View style={styles.optionRow}>
            {sizes.map((size) => (
              <TouchableOpacity
                key={size}
                style={[
                  styles.optionChip,
                  selectedSize === size && styles.optionChipSelected,
                ]}
                onPress={() => setSelectedSize(size)}
              >
                <Text
                  style={[
                    styles.optionChipText,
                    selectedSize === size && styles.optionChipTextSelected,
                  ]}
                >
                  {size}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Color Selection */}
        <View style={styles.optionSection}>
          <Text style={styles.optionTitle}>색상</Text>
          <View style={styles.optionRow}>
            {colors.map((color) => (
              <TouchableOpacity
                key={color}
                style={[
                  styles.optionChip,
                  selectedColor === color && styles.optionChipSelected,
                ]}
                onPress={() => setSelectedColor(color)}
              >
                <Text
                  style={[
                    styles.optionChipText,
                    selectedColor === color && styles.optionChipTextSelected,
                  ]}
                >
                  {color}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Product Description */}
        <View style={styles.descriptionSection}>
          <Text style={styles.descriptionTitle}>상품 설명</Text>
          <Text style={styles.descriptionText}>
            이 상품은 placeholder 상품입니다. 실제 상품 정보가 API에서 로드되면 여기에 표시됩니다.
            {'\n\n'}
            소재: 면 100%{'\n'}
            핏: 레귤러{'\n'}
            시즌: 2026 S/S
          </Text>
        </View>

        {/* Size Guide */}
        <View style={styles.sizeGuideSection}>
          <TouchableOpacity style={styles.sizeGuideButton}>
            <Text style={styles.sizeGuideText}>사이즈 가이드</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

      {/* Bottom CTA */}
      <View style={styles.bottomBar}>
        <TouchableOpacity style={styles.wishButton}>
          <Text style={styles.wishButtonText}>찜</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.cartButton} onPress={handleAddToCart}>
          <Text style={styles.cartButtonText}>장바구니</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.buyButton}>
          <Text style={styles.buyButtonText}>바로구매</Text>
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
  imageContainer: {
    height: 400,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    color: '#999',
    fontSize: 14,
  },
  infoSection: {
    padding: 16,
    borderBottomWidth: 8,
    borderBottomColor: '#f5f5f5',
  },
  brandName: {
    fontSize: 13,
    color: '#999',
  },
  productName: {
    fontSize: 18,
    fontWeight: 'bold',
    marginTop: 4,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 12,
  },
  salePrice: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  originalPrice: {
    fontSize: 14,
    color: '#999',
    textDecorationLine: 'line-through',
  },
  discount: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#e74c3c',
  },
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 8,
  },
  rating: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  reviewCount: {
    fontSize: 13,
    color: '#666',
  },
  optionSection: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  optionTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 12,
  },
  optionRow: {
    flexDirection: 'row',
    gap: 8,
  },
  optionChip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  optionChipSelected: {
    borderColor: '#000',
    backgroundColor: '#000',
  },
  optionChipText: {
    fontSize: 14,
    color: '#333',
  },
  optionChipTextSelected: {
    color: '#fff',
  },
  descriptionSection: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  descriptionTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  descriptionText: {
    fontSize: 14,
    color: '#666',
    lineHeight: 22,
  },
  sizeGuideSection: {
    padding: 16,
  },
  sizeGuideButton: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 14,
    alignItems: 'center',
  },
  sizeGuideText: {
    fontSize: 14,
    color: '#333',
  },
  bottomBar: {
    flexDirection: 'row',
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: '#eee',
    gap: 8,
  },
  wishButton: {
    width: 52,
    height: 48,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  wishButtonText: {
    fontSize: 14,
  },
  cartButton: {
    flex: 1,
    backgroundColor: '#333',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  cartButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold',
  },
  buyButton: {
    flex: 1,
    backgroundColor: '#000',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buyButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold',
  },
});

export default ProductDetailScreen;
