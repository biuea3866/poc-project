import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface ProductOptionSelectorProps {
  title: string;
  options: string[];
  selectedOption: string | null;
  onSelect: (option: string) => void;
}

const ProductOptionSelector: React.FC<ProductOptionSelectorProps> = ({
  title,
  options,
  selectedOption,
  onSelect,
}) => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{title}</Text>
      <View style={styles.optionRow}>
        {options.map((option) => (
          <TouchableOpacity
            key={option}
            style={[
              styles.optionChip,
              selectedOption === option && styles.selectedChip,
            ]}
            onPress={() => onSelect(option)}
          >
            <Text
              style={[
                styles.optionText,
                selectedOption === option && styles.selectedText,
              ]}
            >
              {option}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  title: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 12,
  },
  optionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  optionChip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  selectedChip: {
    borderColor: '#000',
    backgroundColor: '#000',
  },
  optionText: {
    fontSize: 14,
    color: '#333',
  },
  selectedText: {
    color: '#fff',
  },
});

export default ProductOptionSelector;
