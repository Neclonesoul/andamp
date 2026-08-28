export function shuffledIndices(length: number, seed = Date.now()): number[] {
  const result = Array.from({ length }, (_, i) => i);
  let state = (seed >>> 0) || 0x9e3779b9;
  const random = () => {
    state ^= state << 13; state ^= state >>> 17; state ^= state << 5;
    return (state >>> 0) / 0x100000000;
  };
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

export function stableShuffleAroundCurrent<T>(items: readonly T[], currentIndex: number, seed = Date.now()): T[] {
  if (items.length < 2 || currentIndex < 0 || currentIndex >= items.length) return [...items];
  const current = items[currentIndex];
  const rest = items.filter((_, i) => i !== currentIndex);
  const order = shuffledIndices(rest.length, seed).map(i => rest[i]);
  return [current, ...order];
}
