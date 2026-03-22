import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface SizeGuideProps {
  fit?: string;
  sizes: { size: string; chest: string; length: string; shoulder: string }[];
}

const SizeGuide: React.FC<SizeGuideProps> = ({ fit, sizes }) => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>사이즈 가이드</Text>
      {fit && <Text style={styles.fit}>핏: {fit}</Text>}

      <View style={styles.table}>
        <View style={styles.headerRow}>
          <Text style={[styles.cell, styles.headerCell]}>사이즈</Text>
          <Text style={[styles.cell, styles.headerCell]}>가슴</Text>
          <Text style={[styles.cell, styles.headerCell]}>총장</Text>
          <Text style={[styles.cell, styles.headerCell]}>어깨</Text>
        </View>
        {sizes.map((row) => (
          <View key={row.size} style={styles.row}>
            <Text style={[styles.cell, styles.sizeCell]}>{row.size}</Text>
            <Text style={styles.cell}>{row.chest}</Text>
            <Text style={styles.cell}>{row.length}</Text>
            <Text style={styles.cell}>{row.shoulder}</Text>
          </View>
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  title: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  fit: {
    fontSize: 13,
    color: '#666',
    marginBottom: 12,
  },
  table: {
    borderWidth: 1,
    borderColor: '#eee',
    borderRadius: 8,
    overflow: 'hidden',
  },
  headerRow: {
    flexDirection: 'row',
    backgroundColor: '#f5f5f5',
  },
  row: {
    flexDirection: 'row',
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  cell: {
    flex: 1,
    padding: 10,
    textAlign: 'center',
    fontSize: 13,
  },
  headerCell: {
    fontWeight: '600',
    color: '#333',
  },
  sizeCell: {
    fontWeight: '600',
  },
});

export default SizeGuide;
