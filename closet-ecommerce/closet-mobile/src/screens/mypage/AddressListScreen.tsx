import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';

const MOCK_ADDRESSES = [
  {
    id: 1,
    name: '집',
    recipient: '홍길동',
    phoneNumber: '010-1234-5678',
    address: '서울시 강남구 역삼동 123-45',
    addressDetail: '아파트 101동 202호',
    isDefault: true,
  },
  {
    id: 2,
    name: '회사',
    recipient: '홍길동',
    phoneNumber: '010-1234-5678',
    address: '서울시 서초구 서초동 456-78',
    addressDetail: '빌딩 5층',
    isDefault: false,
  },
];

const AddressListScreen: React.FC = () => {
  return (
    <View style={styles.container}>
      <FlatList
        data={MOCK_ADDRESSES}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <View style={styles.addressCard}>
            <View style={styles.addressHeader}>
              <Text style={styles.addressName}>{item.name}</Text>
              {item.isDefault && (
                <View style={styles.defaultBadge}>
                  <Text style={styles.defaultBadgeText}>기본</Text>
                </View>
              )}
            </View>
            <Text style={styles.recipient}>
              {item.recipient} / {item.phoneNumber}
            </Text>
            <Text style={styles.address}>{item.address}</Text>
            <Text style={styles.addressDetail}>{item.addressDetail}</Text>
            <View style={styles.actions}>
              <TouchableOpacity>
                <Text style={styles.actionText}>수정</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={styles.actionText}>삭제</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
        ListFooterComponent={
          <TouchableOpacity style={styles.addButton}>
            <Text style={styles.addButtonText}>+ 새 배송지 추가</Text>
          </TouchableOpacity>
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
  addressCard: {
    backgroundColor: '#fff',
    padding: 16,
    marginBottom: 8,
  },
  addressHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  addressName: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  defaultBadge: {
    backgroundColor: '#000',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  defaultBadgeText: {
    color: '#fff',
    fontSize: 11,
  },
  recipient: {
    fontSize: 14,
    color: '#333',
  },
  address: {
    fontSize: 14,
    color: '#333',
    marginTop: 4,
  },
  addressDetail: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  actions: {
    flexDirection: 'row',
    gap: 16,
    marginTop: 12,
  },
  actionText: {
    fontSize: 13,
    color: '#666',
  },
  addButton: {
    backgroundColor: '#fff',
    padding: 16,
    alignItems: 'center',
    marginBottom: 32,
  },
  addButtonText: {
    fontSize: 14,
    color: '#333',
    fontWeight: '600',
  },
});

export default AddressListScreen;
