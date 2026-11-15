const DEFAULT_TIMEOUT_MS = 15_000
const CSRF_COOKIE = "XSRF-TOKEN"
const CSRF_HEADER = "X-XSRF-TOKEN"
const CSRF_TITLE = "Invalid CSRF token"
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"])

/** Error raised for any failed API call, carrying a message that is safe to show to the user. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>
  readonly title: string | undefined

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
    title?: string
  ) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.fieldErrors = fieldErrors
    this.title = title
  }
}

interface ProblemBody {
  title?: string
  detail?: string
  errors?: Record<string, string>
}

export interface ApiRequestInit extends Omit<RequestInit, "body"> {
  body?: unknown
  timeoutMs?: number
}

let onUnauthorized: (() => void) | null = null

/**
 * Registers what happens when the server reports that the session has ended (HTTP 401),
 * for example sending the user to the sign-in page. Returns a function that removes the handler.
 */
export function setUnauthorizedHandler(handler: () => void): () => void {
  onUnauthorized = handler
  return () => {
    if (onUnauthorized === handler) onUnauthorized = null
  }
}

function withTimeout(signal: AbortSignal | null | undefined, timeoutMs: number) {
  const timeout = AbortSignal.timeout(timeoutMs)
  if (!signal) return timeout
  return typeof AbortSignal.any === "function" ? AbortSignal.any([signal, timeout]) : signal
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null
  const prefix = `${name}=`
  const match = document.cookie.split("; ").find((part) => part.startsWith(prefix))
  return match ? decodeURIComponent(match.slice(prefix.length)) : null
}

/** Returns the CSRF token from its cookie, asking the server to set the cookie when it is missing. */
async function csrfToken(refresh = false): Promise<string | null> {
  const existing = refresh ? null : readCookie(CSRF_COOKIE)
  if (existing) return existing
  try {
    await fetch("/api/auth/csrf", { headers: { Accept: "application/json" } })
  } catch {
    // The request that needs the token reports the connection problem.
  }
  return readCookie(CSRF_COOKIE)
}

async function toApiError(res: Response): Promise<ApiError> {
  let problem: ProblemBody = {}
  try {
    problem = (await res.json()) as ProblemBody
  } catch {
    // Non-JSON error bodies (for example a proxy error page) fall back to the status text.
  }
  const fieldErrors = problem.errors ?? {}
  const fieldSummary = Object.entries(fieldErrors)
    .map(([field, message]) => `${field} ${message}`)
    .join("; ")
  const message =
    fieldSummary ||
    problem.detail ||
    problem.title ||
    `Request failed with status ${res.status}${res.statusText ? ` (${res.statusText})` : ""}`
  return new ApiError(message, res.status, fieldErrors, problem.title)
}

/**
 * Calls the backend API with a timeout and turns failures into {@link ApiError}.
 * Plain objects passed as `body` are sent as JSON. Requests that change data carry the CSRF
 * token, and are retried once with a fresh token when the server rejects an outdated one.
 */
export async function apiFetch<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, body, headers, signal, ...rest } = init
  const hasBody = body !== undefined
  const needsCsrf = !SAFE_METHODS.has((rest.method ?? "GET").toUpperCase())

  const send = async (token: string | null) => {
    try {
      return await fetch(path, {
        ...rest,
        headers: {
          Accept: "application/json",
          ...(hasBody ? { "Content-Type": "application/json" } : {}),
          ...(token ? { [CSRF_HEADER]: token } : {}),
          ...headers,
        },
        body: hasBody ? JSON.stringify(body) : undefined,
        signal: withTimeout(signal, timeoutMs),
      })
    } catch (e) {
      if (e instanceof DOMException && e.name === "TimeoutError") {
        throw new ApiError("The server took too long to respond. Please try again.", 0)
      }
      if (e instanceof DOMException && e.name === "AbortError") {
        throw e
      }
      throw new ApiError("Cannot reach the server. Check that the backend is running.", 0)
    }
  }

  let res = await send(needsCsrf ? await csrfToken() : null)
  if (!res.ok) {
    let error = await toApiError(res)
    if (needsCsrf && error.status === 403 && error.title === CSRF_TITLE) {
      res = await send(await csrfToken(true))
      error = res.ok ? error : await toApiError(res)
    }
    if (!res.ok) {
      if (error.status === 401 && !path.startsWith("/api/auth/")) {
        onUnauthorized?.()
      }
      throw error
    }
  }
  if (res.status === 204) {
    return undefined as T
  }
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export function errorMessage(e: unknown): string {
  if (e instanceof Error && e.message) return e.message
  return "Something went wrong. Please try again."
}
