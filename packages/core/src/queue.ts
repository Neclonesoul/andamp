export function moveItem<T>(items: readonly T[], from: number, to: number): T[] {
  if (from < 0 || to < 0 || from >= items.length || to >= items.length) return [...items];
  const next = [...items];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
}

export function insertNext<T>(items: readonly T[], currentIndex: number, item: T): T[] {
  const next = [...items];
  next.splice(Math.min(currentIndex + 1, next.length), 0, item);
  return next;
}
