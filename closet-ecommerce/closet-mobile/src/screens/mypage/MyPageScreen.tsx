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
import { useAuth } from '../../hooks/useAuth';
import { formatPrice } from '../../utils/format';

const MyPageScreen: React.FC = () => {
  const navigation = useNavigation<RootNavigationProp>();
  const { user, logout } = useAuth();

  const menuItems = [
    { label: '주문/배송 조회', onPress: () => { /* TODO: navigate to OrderList */ } },
    { label: '배송지 관리', onPress: () => { /* TODO: navigate to AddressList */ } },
    { label: '쿠폰함', onPress: () => {} },
    { label: '포인트', onPress: () => {} },
    { label: '찜한 상품', onPress: () => {} },
    { label: '최근 본 상품', onPress: () => {} },
    { label: '상품 리뷰', onPress: () => {} },
    { label: '1:1 문의', onPress: () => {} },
    { label: '공지사항', onPress: () => {} },
    { label: 'FAQ', onPress: () => {} },
  ];

  return (
    <ScrollView style={styles.container}>
      {/* Profile Section */}
      <View style={styles.profileSection}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {user?.name?.[0] || '?'}
          </Text>
        </View>
        <View style={styles.profileInfo}>
          <Text style={styles.userName}>{user?.name || '사용자'}</Text>
          <Text style={styles.userGrade}>{user?.grade || 'NORMAL'} 회원</Text>
          <Text style={styles.userEmail}>{user?.email || 'user@example.com'}</Text>
        </View>
      </View>

      {/* Quick Stats */}
      <View style={styles.statsSection}>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{formatPrice(user?.point || 0)}</Text>
          <Text style={styles.statLabel}>포인트</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>3장</Text>
          <Text style={styles.statLabel}>쿠폰</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>5개</Text>
          <Text style={styles.statLabel}>찜</Text>
        </View>
      </View>

      {/* Order Status Quick View */}
      <View style={styles.orderStatusSection}>
        <Text style={styles.sectionTitle}>주문/배송</Text>
        <View style={styles.orderStatusRow}>
          {[
            { label: '결제완료', count: 1 },
            { label: '준비중', count: 0 },
            { label: '배송중', count: 1 },
            { label: '배송완료', count: 3 },
          ].map((item) => (
            <View key={item.label} style={styles.orderStatusItem}>
              <Text style={styles.orderStatusCount}>{item.count}</Text>
              <Text style={styles.orderStatusLabel}>{item.label}</Text>
            </View>
          ))}
        </View>
      </View>

      {/* Menu List */}
      <View style={styles.menuSection}>
        {menuItems.map((item) => (
          <TouchableOpacity
            key={item.label}
            style={styles.menuItem}
            onPress={item.onPress}
          >
            <Text style={styles.menuLabel}>{item.label}</Text>
            <Text style={styles.menuArrow}>{'>'}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Logout */}
      <TouchableOpacity style={styles.logoutButton} onPress={logout}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  profileSection: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 20,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#fff',
    fontSize: 22,
    fontWeight: 'bold',
  },
  profileInfo: {
    marginLeft: 16,
  },
  userName: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  userGrade: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  userEmail: {
    fontSize: 12,
    color: '#999',
    marginTop: 2,
  },
  statsSection: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    paddingVertical: 16,
    marginTop: 1,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  statLabel: {
    fontSize: 12,
    color: '#999',
    marginTop: 4,
  },
  statDivider: {
    width: 1,
    backgroundColor: '#eee',
  },
  orderStatusSection: {
    backgroundColor: '#fff',
    padding: 16,
    marginTop: 8,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  orderStatusRow: {
    flexDirection: 'row',
  },
  orderStatusItem: {
    flex: 1,
    alignItems: 'center',
  },
  orderStatusCount: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  orderStatusLabel: {
    fontSize: 11,
    color: '#666',
    marginTop: 4,
  },
  menuSection: {
    backgroundColor: '#fff',
    marginTop: 8,
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  menuLabel: {
    fontSize: 15,
    color: '#333',
  },
  menuArrow: {
    fontSize: 14,
    color: '#ccc',
  },
  logoutButton: {
    backgroundColor: '#fff',
    marginTop: 8,
    marginBottom: 32,
    padding: 16,
    alignItems: 'center',
  },
  logoutText: {
    color: '#999',
    fontSize: 14,
  },
});

export default MyPageScreen;
