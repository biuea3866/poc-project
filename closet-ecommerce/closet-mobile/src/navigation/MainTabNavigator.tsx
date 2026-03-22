import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Text } from 'react-native';
import { MainTabParamList } from './types';
import HomeScreen from '../screens/home/HomeScreen';
import ProductListScreen from '../screens/product/ProductListScreen';
import CartScreen from '../screens/cart/CartScreen';
import MyPageScreen from '../screens/mypage/MyPageScreen';
import { useCart } from '../hooks/useCart';

const Tab = createBottomTabNavigator<MainTabParamList>();

const MainTabNavigator: React.FC = () => {
  const { itemCount } = useCart();

  return (
    <Tab.Navigator
      screenOptions={{
        tabBarActiveTintColor: '#000',
        tabBarInactiveTintColor: '#999',
        tabBarStyle: {
          backgroundColor: '#fff',
          borderTopColor: '#eee',
        },
        headerStyle: { backgroundColor: '#fff' },
        headerTintColor: '#000',
        headerTitleStyle: { fontWeight: 'bold' },
      }}
    >
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{
          title: '홈',
          tabBarIcon: ({ color }) => <Text style={{ color, fontSize: 20 }}>🏠</Text>,
        }}
      />
      <Tab.Screen
        name="Category"
        component={ProductListScreen}
        options={{
          title: '카테고리',
          tabBarIcon: ({ color }) => <Text style={{ color, fontSize: 20 }}>📂</Text>,
        }}
      />
      <Tab.Screen
        name="Cart"
        component={CartScreen}
        options={{
          title: '장바구니',
          tabBarIcon: ({ color }) => <Text style={{ color, fontSize: 20 }}>🛒</Text>,
          tabBarBadge: itemCount > 0 ? itemCount : undefined,
        }}
      />
      <Tab.Screen
        name="MyPage"
        component={MyPageScreen}
        options={{
          title: '마이페이지',
          tabBarIcon: ({ color }) => <Text style={{ color, fontSize: 20 }}>👤</Text>,
        }}
      />
    </Tab.Navigator>
  );
};

export default MainTabNavigator;
