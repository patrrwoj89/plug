export function intToToken(value: number, radix: number): string {
  if (value < radix) return charFor(value);
  let v = value;
  let result = '';
  while (v > 0) {
    result = charFor(v % radix) + result;
    v = Math.floor(v / radix);
  }
  return result;
}

function charFor(value: number): string {
  if (value < 10) return String.fromCharCode('0'.charCodeAt(0) + value);
  if (value < 36) return String.fromCharCode('a'.charCodeAt(0) + (value - 10));
  if (value < 62) return String.fromCharCode('A'.charCodeAt(0) + (value - 36));
  return '?';
}
