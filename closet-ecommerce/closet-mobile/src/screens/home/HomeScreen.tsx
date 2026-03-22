import React from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { RootNavigationProp } from '../../navigation/types';

const HomeScreen: React.FC = () => {
  const navigation = useNavigation<RootNavigationProp>();

  return (
    <ScrollView style={styles.container}>
      {/* Banner Area */}
      <View style={styles.banner}>
        <Text style={styles.bannerText}>CLOSET</Text>
        <Text style={styles.bannerSubText}>2026 S/S 신상품 컬렉션</Text>
      </View>

      {/* Quick Categories */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>카테고리</Text>
        <View style={styles.categoryGrid}>
          {['상의', '하의', '아우터', '신발', '액세서리', '가방'].map((cat) => (
            <TouchableOpacity key={cat} style={styles.categoryItem}>
              <View style={styles.categoryIcon}>
                <Text style={styles.categoryIconText}>{cat[0]}</Text>
              </View>
              <Text style={styles.categoryName}>{cat}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Popular Products Placeholder */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>인기 상품</Text>
        <View style={styles.productGrid}>
          {[1, 2, 3, 4].map((id) => (
            <TouchableOpacity
              key={id}
              style={styles.productCard}
              onPress={() => navigation.navigate('ProductDetail', { productId: id })}
            >
              <View style={styles.productImage}>
                <Text style={styles.placeholderText}>상품 이미지</Text>
              </View>
              <Text style={styles.productBrand}>브랜드명</Text>
              <Text style={styles.productName} numberOfLines={2}>
                상품명 placeholder #{id}
              </Text>
              <Text style={styles.productPrice}>59,000원</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* New Arrivals Placeholder */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>신상품</Text>
        <View style={styles.productGrid}>
          {[5, 6, 7, 8].map((id) => (
            <TouchableOpacity
              key={id}
              style={styles.productCard}
              onPress={() => navigation.navigate('ProductDetail', { productId: id })}
            >
              <View style={styles.productImage}>
                <Text style={styles.placeholderText}>상품 이미지</Text>
              </View>
              <Text style={styles.productBrand}>브랜드명</Text>
              <Text style={styles.productName} numberOfLines={2}>
                신상품 placeholder #{id}
              </Text>
              <Text style={styles.productPrice}>79,000원</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  banner: {
    height: 200,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  bannerText: {
    color: '#fff',
    fontSize: 32,
    fontWeight: 'bold',
    letterSpacing: 6,
  },
  bannerSubText: {
    color: '#ccc',
    fontSize: 14,
    marginTop: 8,
  },
  section: {
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
  },
  categoryItem: {
    alignItems: 'center',
    width: 60,
  },
  categoryIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  categoryIconText: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  categoryName: {
    fontSize: 12,
    marginTop: 4,
    color: '#333',
  },
  productGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  productCard: {
    width: '47%',
  },
  productImage: {
    height: 180,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    color: '#999',
    fontSize: 12,
  },
  productBrand: {
    fontSize: 11,
    color: '#999',
    marginTop: 8,
  },
  productName: {
    fontSize: 13,
    color: '#333',
    marginTop: 2,
  },
  productPrice: {
    fontSize: 14,
    fontWeight: 'bold',
    marginTop: 4,
  },
});

export default HomeScreen;
