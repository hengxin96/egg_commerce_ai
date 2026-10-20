export const UPLOAD_LIMIT = 6

export function selectImageFiles(files, currentCount, limit = UPLOAD_LIMIT) {
  const remaining = Math.max(limit - currentCount, 0)

  if (!remaining) {
    return []
  }

  return Array.from(files ?? [])
    .filter((file) => file?.type?.startsWith('image/'))
    .slice(0, remaining)
}
