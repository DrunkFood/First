export const storage = {
  get(key: string): any {
    const value = localStorage.getItem(key)
    if (!value) return null
    try {
      return JSON.parse(value)
    } catch {
      return value
    }
  },
  set(key: string, value: any): void {
    localStorage.setItem(key, JSON.stringify(value))
  },
  remove(key: string): void {
    localStorage.removeItem(key)
  },
}
