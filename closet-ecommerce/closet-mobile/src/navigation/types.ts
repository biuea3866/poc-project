import { StackNavigationProp } from '@react-navigation/stack';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, RouteProp } from '@react-navigation/native';

// Root Stack
export type RootStackParamList = {
  Auth: undefined;
  Main: undefined;
  ProductDetail: { productId: number };
  Checkout: { cartItemIds: number[] };
  OrderDetail: { orderId: number };
};

// Auth Stack
export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

// Main Tab
export type MainTabParamList = {
  Home: undefined;
  Category: undefined;
  Cart: undefined;
  MyPage: undefined;
};

// MyPage Stack (nested in tab)
export type MyPageStackParamList = {
  MyPageMain: undefined;
  AddressList: undefined;
  OrderList: undefined;
};

// Navigation Props
export type RootNavigationProp = StackNavigationProp<RootStackParamList>;

export type AuthNavigationProp = CompositeNavigationProp<
  StackNavigationProp<AuthStackParamList>,
  StackNavigationProp<RootStackParamList>
>;

export type MainTabNavigationProp = CompositeNavigationProp<
  BottomTabNavigationProp<MainTabParamList>,
  StackNavigationProp<RootStackParamList>
>;

// Route Props
export type ProductDetailRouteProp = RouteProp<RootStackParamList, 'ProductDetail'>;
export type CheckoutRouteProp = RouteProp<RootStackParamList, 'Checkout'>;
export type OrderDetailRouteProp = RouteProp<RootStackParamList, 'OrderDetail'>;
