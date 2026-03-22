import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { AuthNavigationProp } from '../../navigation/types';
import { memberApi } from '../../api/member';

const RegisterScreen: React.FC = () => {
  const navigation = useNavigation<AuthNavigationProp>();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [nickname, setNickname] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleRegister = async () => {
    if (!email || !password || !name) {
      Alert.alert('알림', '필수 항목을 입력해주세요.');
      return;
    }
    if (password !== confirmPassword) {
      Alert.alert('알림', '비밀번호가 일치하지 않습니다.');
      return;
    }

    setIsLoading(true);
    try {
      await memberApi.register({ email, password, name, nickname, phoneNumber });
      Alert.alert('가입 완료', '회원가입이 완료되었습니다. 로그인해주세요.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ]);
    } catch {
      Alert.alert('가입 실패', '회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>회원가입</Text>

        <View style={styles.form}>
          <Text style={styles.label}>이메일 *</Text>
          <TextInput
            style={styles.input}
            placeholder="이메일을 입력해주세요"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
          />

          <Text style={styles.label}>비밀번호 *</Text>
          <TextInput
            style={styles.input}
            placeholder="비밀번호를 입력해주세요"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <Text style={styles.label}>비밀번호 확인 *</Text>
          <TextInput
            style={styles.input}
            placeholder="비밀번호를 다시 입력해주세요"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            secureTextEntry
          />

          <Text style={styles.label}>이름 *</Text>
          <TextInput
            style={styles.input}
            placeholder="이름을 입력해주세요"
            value={name}
            onChangeText={setName}
          />

          <Text style={styles.label}>닉네임</Text>
          <TextInput
            style={styles.input}
            placeholder="닉네임을 입력해주세요"
            value={nickname}
            onChangeText={setNickname}
          />

          <Text style={styles.label}>전화번호</Text>
          <TextInput
            style={styles.input}
            placeholder="전화번호를 입력해주세요"
            value={phoneNumber}
            onChangeText={setPhoneNumber}
            keyboardType="phone-pad"
          />

          <TouchableOpacity
            style={[styles.registerButton, isLoading && styles.disabledButton]}
            onPress={handleRegister}
            disabled={isLoading}
          >
            <Text style={styles.registerButtonText}>
              {isLoading ? '가입 중...' : '가입하기'}
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    padding: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 24,
  },
  form: {
    gap: 8,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginTop: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 14,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  registerButton: {
    backgroundColor: '#000',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 16,
  },
  disabledButton: {
    backgroundColor: '#999',
  },
  registerButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default RegisterScreen;
