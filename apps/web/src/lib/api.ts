export type User = {
  id: number
  name: string
  email: string
  profileImageUrl?: string | null
}

export type Post = {
  id: number
  content: string
  mediaUrl?: string | null
  userId: number
  createdAt: string
}

export type Person = {
  id: number
  userId: number
  name: string
}

export type Notice = {
  id: number
  userId: number
  type: string
  message: string
  referenceId: number
  createdAt: string
}

const TOKEN_KEY = 'circlo.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(path, { ...init, headers })
  const raw = await response.text()
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    if (raw) {
      try {
        const body = JSON.parse(raw) as { error?: string; message?: string }
        message = body.error || body.message || raw
      } catch {
        message = raw
      }
    }
    throw new Error(message)
  }
  if (!raw) return undefined as T
  try {
    return JSON.parse(raw) as T
  } catch {
    return raw as T
  }
}

export const api = {
  signup(body: { name: string; email: string; password: string }) {
    return request<User>('/api/v1/users/auth/signup', { method: 'POST', body: JSON.stringify(body) })
  },
  login(body: { email: string; password: string }) {
    return request<string>('/api/v1/users/auth/login', { method: 'POST', body: JSON.stringify(body) })
  },
  forgot(body: { email: string; newPassword: string }) {
    return request<void>('/api/v1/users/auth/forgot-password', { method: 'POST', body: JSON.stringify(body) })
  },
  me() {
    return request<User>('/api/v1/users/profile/me')
  },
  people() {
    return request<User[]>('/api/v1/users/people')
  },
  feed() {
    return request<Post[]>('/api/v1/posts/core/feed')
  },
  createPost(body: { content: string; mediaUrl?: string }) {
    return request<Post>('/api/v1/posts/core', { method: 'POST', body: JSON.stringify(body) })
  },
  like(postId: number) {
    return request<void>(`/api/v1/posts/likes/${postId}`, { method: 'POST' })
  },
  unlike(postId: number) {
    return request<void>(`/api/v1/posts/likes/${postId}`, { method: 'DELETE' })
  },
  notices() {
    return request<Notice[]>('/api/v1/notifications/me')
  },
  connections(userId: number) {
    return request<Person[]>(`/api/v1/connections/core/${userId}/first-degree`)
  },
  sendRequest(userId: number) {
    return request<void>(`/api/v1/connections/core/requests/${userId}`, { method: 'POST' })
  },
  cancelRequest(userId: number) {
    return request<void>(`/api/v1/connections/core/requests/${userId}`, { method: 'DELETE' })
  },
  acceptRequest(userId: number) {
    return request<void>(`/api/v1/connections/core/requests/${userId}/accept`, { method: 'POST' })
  },
  declineRequest(userId: number) {
    return request<void>(`/api/v1/connections/core/requests/${userId}/decline`, { method: 'POST' })
  },
  async upload(file: File, usage: 'profile' | 'post') {
    const data = new FormData()
    data.set('file', file)
    data.set('usage', usage)
    return request<{ url: string }>(`/api/v1/uploads/images?usage=${usage}`, { method: 'POST', body: data })
  },
}
