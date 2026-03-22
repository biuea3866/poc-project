import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';

interface BadgeProps {
  text: string;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info';
  style?: ViewStyle;
}

const variantColors = {
  default: { bg: '#f0f0f0', text: '#333' },
  success: { bg: '#e8f5e9', text: '#2e7d32' },
  warning: { bg: '#fff3e0', text: '#e65100' },
  danger: { bg: '#ffebee', text: '#c62828' },
  info: { bg: '#e3f2fd', text: '#1565c0' },
};

const Badge: React.FC<BadgeProps> = ({ text, variant = 'default', style }) => {
  const colors = variantColors[variant];

  return (
    <View style={[styles.badge, { backgroundColor: colors.bg }, style]}>
      <Text style={[styles.text, { color: colors.text }]}>{text}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
    alignSelf: 'flex-start',
  },
  text: {
    fontSize: 12,
    fontWeight: '600',
  },
});

export default Badge;
