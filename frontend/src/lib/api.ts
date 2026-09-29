import type { CurrentUser } from '@/types'

type CsrfResponse = {
  headerName: string
  parameterName: string
  token: string
}

type ApiErrorBody = {
  message?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

let csrfToken: CsrfResponse | null = null

async function readError(response: Response): Promise<ApiError> {
  const body = (await response.json().catch(() => ({}))) as ApiErrorBody
  return new ApiError(body.message ?? '요청 처리 중 오류가 발생했습니다.', response.status, body.fieldErrors)
}

export async function refreshCsrfToken(): Promise<CsrfResponse> {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) {
    throw await readError(response)
  }
  csrfToken = (await response.json()) as CsrfResponse
  return csrfToken
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrfToken ?? (await refreshCsrfToken())
    headers.set(token.headerName, token.token)
  }

  if (init.body && !(init.body instanceof URLSearchParams) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers,
  })

  if (!response.ok) {
    throw await readError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export async function login(username: string, password: string): Promise<void> {
  const token = await refreshCsrfToken()
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [token.headerName]: token.token,
    },
    body: new URLSearchParams({ username, password }),
  })

  if (!response.ok) {
    throw await readError(response)
  }

  csrfToken = null
  await refreshCsrfToken()
}

export async function logout(): Promise<void> {
  await apiFetch<void>('/api/auth/logout', { method: 'POST' })
  csrfToken = null
  await refreshCsrfToken()
}

export function getCurrentUser(): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/auth/me')
}

export function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '요청 처리 중 오류가 발생했습니다.'
}
