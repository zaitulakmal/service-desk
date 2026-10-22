import { useMemo } from 'react'
import { useAuth } from 'react-oidc-context'
import { config } from '../config'

/** An error response from the API (RFC 9457 problem detail). */
export class ApiError extends Error {
  readonly status: number
  /** Field name → message, for showing errors next to form fields. */
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

interface ProblemDetail {
  detail?: string
  title?: string
  errors?: Record<string, string>
}

export interface Api {
  /** Pass the query's AbortSignal so a cancelled query also stops its HTTP request. */
  get<T>(path: string, signal?: AbortSignal): Promise<T>
  post<T>(path: string, body?: unknown): Promise<T>
  patch<T>(path: string, body: unknown): Promise<T>
  download(path: string, fileName: string): Promise<void>
}

/** Calls the API as the signed-in user, adding their access token to every request. */
export function useApi(): Api {
  const auth = useAuth()
  const token = auth.user?.access_token

  return useMemo(() => {
    async function send(path: string, init: RequestInit = {}): Promise<Response> {
      const headers = new Headers(init.headers)
      headers.set('Authorization', `Bearer ${token}`)
      if (init.body !== undefined && !(init.body instanceof FormData)) {
        headers.set('Content-Type', 'application/json')
      }
      const response = await fetch(config.apiUrl + path, { ...init, headers })
      if (!response.ok) {
        const problem = (await response.json().catch(() => ({}))) as ProblemDetail
        throw new ApiError(response.status, problem.detail ?? problem.title ?? response.statusText, problem.errors)
      }
      return response
    }

    async function json<T>(path: string, init?: RequestInit): Promise<T> {
      const response = await send(path, init)
      return (await response.json()) as T
    }

    function body(value: unknown): BodyInit | undefined {
      if (value === undefined) return undefined
      return value instanceof FormData ? value : JSON.stringify(value)
    }

    return {
      get: <T>(path: string, signal?: AbortSignal) => json<T>(path, { signal }),
      post: <T>(path: string, value?: unknown) => json<T>(path, { method: 'POST', body: body(value) }),
      patch: <T>(path: string, value: unknown) => json<T>(path, { method: 'PATCH', body: body(value) }),
      async download(path: string, fileName: string) {
        const response = await send(path)
        const url = URL.createObjectURL(await response.blob())
        const link = document.createElement('a')
        link.href = url
        link.download = fileName
        link.click()
        URL.revokeObjectURL(url)
      },
    }
  }, [token])
}
