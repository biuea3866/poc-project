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

const MOCK_PRODUCTS = Array.from({ length: 20 }, (_, i) => ({
  id: i + 1,
  name: `상품 ${i + 1}`,
  brandName: `브랜드 ${(i % 5) + 1}`,
  price: (i + 1) * 10000 + 9000,
}));

const ProductListScreen: React.FC = () => {
  const navigation = useNavigation<RootNavigationProp>();

  return (
    <View style={styles.container}>
      {/* Filter Bar */}
      <View style={styles.filterBar}>
        {['전체', '상의', '하의', '아우터', '신발'].map((filter) => (
          <TouchableOpacity key={filter} style={styles.filterChip}>
            <Text style={styles.filterText}>{filter}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Sort */}
      <View style={styles.sortBar}>
        <Text style={styles.resultCount}>총 {MOCK_PRODUCTS.length}개</Text>
        <TouchableOpacity>
          <Text style={styles.sortText}>인기순</Text>
        </TouchableOpacity>
      </View>

      {/* Product List */}
      <FlatList
        data={MOCK_PRODUCTS}
        keyExtractor={(item) => String(item.id)}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.productCard}
            onPress={() => navigation.navigate('ProductDetail', { productId: item.id })}
          >
            <View style={styles.productImage}>
              <Text style={styles.placeholderText}>이미지</Text>
            </View>
            <Text style={styles.brandName}>{item.brandName}</Text>
            <Text style={styles.productName} numberOfLines={2}>
              {item.name}
            </Text>
            <Text style={styles.price}>{item.price.toLocaleString()}원</Text>
          </TouchableOpacity>
        )}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  filterBar: {
    flexDirection: 'row',
    padding: 12,
    gap: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  filterChip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: '#f0f0f0',
  },
  filterText: {
    fontSize: 13,
    color: '#333',
  },
  sortBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  resultCount: {
    fontSize: 13,
    color: '#666',
  },
  sortText: {
    fontSize: 13,
    color: '#333',
  },
  list: {
    padding: 8,
  },
  row: {
    gap: 8,
  },
  productCard: {
    flex: 1,
    padding: 8,
  },
  productImage: {
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
  brandName: {
    fontSize: 11,
    color: '#999',
    marginTop: 8,
  },
  productName: {
    fontSize: 13,
    color: '#333',
    marginTop: 2,
  },
  price: {
    fontSize: 14,
    fontWeight: 'bold',
    marginTop: 4,
  },
});

export default ProductListScreen;
