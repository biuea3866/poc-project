import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  Alert,
} from 'react-native';
import { useRoute, useNavigation } from '@react-navigation/native';
import { CheckoutRouteProp, RootNavigationProp } from '../../navigation/types';
import { formatPrice } from '../../utils/format';

const CheckoutScreen: React.FC = () => {
  const route = useRoute<CheckoutRouteProp>();
  const navigation = useNavigation<RootNavigationProp>();
  const { cartItemIds } = route.params;

  const [recipient, setRecipient] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [address, setAddress] = useState('');
  const [addressDetail, setAddressDetail] = useState('');
  const [selectedPayment, setSelectedPayment] = useState<string | null>(null);

  const paymentMethods = [
    { key: 'CARD', label: '신용/체크카드' },
    { key: 'KAKAO_PAY', label: '카카오페이' },
    { key: 'NAVER_PAY', label: '네이버페이' },
    { key: 'TOSS_PAY', label: '토스페이' },
    { key: 'BANK_TRANSFER', label: '무통장입금' },
  ];

  const handleOrder = () => {
    if (!recipient || !phoneNumber || !address) {
      Alert.alert('알림', '배송지 정보를 입력해주세요.');
      return;
    }
    if (!selectedPayment) {
      Alert.alert('알림', '결제 수단을 선택해주세요.');
      return;
    }
    Alert.alert('주문 확인', '주문을 진행하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      {
        text: '확인',
        onPress: () => {
          // TODO: Call order API
          Alert.alert('주문 완료', '주문이 완료되었습니다.', [
            { text: '확인', onPress: () => navigation.navigate('Main') },
          ]);
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <ScrollView>
        {/* Order Items Summary */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>주문 상품</Text>
          <Text style={styles.itemSummary}>
            총 {cartItemIds.length}개 상품
          </Text>
        </View>

        {/* Shipping Address */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>배송지 정보</Text>
          <TextInput
            style={styles.input}
            placeholder="수령인"
            value={recipient}
            onChangeText={setRecipient}
          />
          <TextInput
            style={styles.input}
            placeholder="연락처"
            value={phoneNumber}
            onChangeText={setPhoneNumber}
            keyboardType="phone-pad"
          />
          <TextInput
            style={styles.input}
            placeholder="주소"
            value={address}
            onChangeText={setAddress}
          />
          <TextInput
            style={styles.input}
            placeholder="상세 주소"
            value={addressDetail}
            onChangeText={setAddressDetail}
          />
        </View>

        {/* Payment Method */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>결제 수단</Text>
          {paymentMethods.map((method) => (
            <TouchableOpacity
              key={method.key}
              style={[
                styles.paymentOption,
                selectedPayment === method.key && styles.paymentOptionSelected,
              ]}
              onPress={() => setSelectedPayment(method.key)}
            >
              <View style={styles.radio}>
                {selectedPayment === method.key && <View style={styles.radioInner} />}
              </View>
              <Text style={styles.paymentLabel}>{method.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Order Summary */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>결제 금액</Text>
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>상품 금액</Text>
            <Text style={styles.summaryValue}>{formatPrice(59000)}</Text>
          </View>
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>배송비</Text>
            <Text style={styles.summaryValue}>무료</Text>
          </View>
          <View style={[styles.summaryRow, styles.totalRow]}>
            <Text style={styles.totalLabel}>총 결제 금액</Text>
            <Text style={styles.totalValue}>{formatPrice(59000)}</Text>
          </View>
        </View>
      </ScrollView>

      <View style={styles.bottomBar}>
        <TouchableOpacity style={styles.orderButton} onPress={handleOrder}>
          <Text style={styles.orderButtonText}>
            {formatPrice(59000)} 결제하기
          </Text>
        </TouchableOpacity>
      </View>
    </View>
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
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  itemSummary: {
    fontSize: 14,
    color: '#666',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 14,
    fontSize: 14,
    marginBottom: 8,
    backgroundColor: '#f9f9f9',
  },
  paymentOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  paymentOptionSelected: {
    backgroundColor: '#f9f9f9',
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#ddd',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  radioInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#000',
  },
  paymentLabel: {
    fontSize: 14,
    color: '#333',
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
  bottomBar: {
    padding: 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  orderButton: {
    backgroundColor: '#000',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
  },
  orderButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default CheckoutScreen;
