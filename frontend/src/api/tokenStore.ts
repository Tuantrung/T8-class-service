// Lightweight token holder to break the circular import between authStore and client.
// authStore writes here; client.ts reads from here.
let _token: string | null = null

export const tokenStore = {
  get: () => _token,
  set: (t: string | null) => { _token = t },
}
