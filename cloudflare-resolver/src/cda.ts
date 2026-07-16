const SPECIAL_TO_DIGIT: Record<string, string> = {
  '!': '0',
  '@': '1',
  '#': '2',
  '$': '3',
  '%': '4'
};

export function cdaDecode(input: string): string {
  if (!input) return input;

  const stripped = input.replace(/_b64/g, '');
  const digitized = Array.from(stripped).map(c => SPECIAL_TO_DIGIT[c] ?? c).join('');
  const decoded = decodeURIComponentSafe(digitized);

  return decoded.split('').map(c => {
    const code = c.charCodeAt(0);
    if (code >= 33 && code <= 126) {
      return String.fromCharCode(((code - 33 + 47) % 94) + 33);
    }
    return c;
  }).join('');
}

function decodeURIComponentSafe(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
