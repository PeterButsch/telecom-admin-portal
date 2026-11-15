const DEFAULT_TIMEOUT_MS = 15_000

/** Error raised for any failed API call, carrying a message that is safe to show to the user. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.fieldErrors = fieldErrors
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

function withTimeout(signal: AbortSignal | null | undefined, timeoutMs: number) {
  const timeout = AbortSignal.timeout(timeoutMs)
  if (!signal) return timeout
  return typeof AbortSignal.any === "function" ? AbortSignal.any([signal, timeout]) : signal
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
  return new ApiError(message, res.status, fieldErrors)
}

/**
 * Calls the backend API with a timeout and turns failures into {@link ApiError}.
 * Plain objects passed as `body` are sent as JSON.
 */
export async function apiFetch<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, body, headers, signal, ...rest } = init
  const hasBody = body !== undefined
  let res: Response
  try {
    res = await fetch(path, {
      ...rest,
      headers: {
        Accept: "application/json",
        ...(hasBody ? { "Content-Type": "application/json" } : {}),
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
  if (!res.ok) {
    throw await toApiError(res)
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
