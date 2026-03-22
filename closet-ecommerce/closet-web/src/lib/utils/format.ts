/**
 * Format price as Korean Won (e.g., "1,000")
 */
export function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price);
}

/**
 * Format price with currency symbol (e.g., "₩1,000")
 */
export function formatPriceWithCurrency(price: number): string {
  return `₩${formatPrice(price)}`;
}

/**
 * Format date string to Korean locale (e.g., "2026.03.22")
 */
export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).replace(/\. /g, '.').replace(/\.$/, '');
}

/**
 * Format date with time (e.g., "2026.03.22 14:30")
 */
export function formatDateTime(dateString: string): string {
  const date = new Date(dateString);
  return `${formatDate(dateString)} ${date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })}`;
}
