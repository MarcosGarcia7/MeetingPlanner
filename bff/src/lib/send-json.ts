import {readApiError} from './api-error';

export type SendResult<T> =
  | {ok: true; data: T}
  | {ok: false; message: string; unauthenticated: boolean};

// Client-side call to a BFF /api route. Resolves (never throws) with the parsed body, or with the API's
// error message; `unauthenticated` means the session is gone and the caller should go to /login.
// A 204 resolves with `undefined` as data.
export async function sendJson<T = void>(url: string, method = 'GET', body?: object): Promise<SendResult<T>> {
  try {
    const response = await fetch(url, {
      method,
      headers: body ? {'Content-Type': 'application/json'} : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
      const apiError = await readApiError(response);
      return {
        ok: false,
        message: apiError?.message ?? 'Something went wrong. Please try again.',
        unauthenticated: apiError?.error === 'unauthenticated',
      };
    }
    const data = response.status === 204 ? undefined : await response.json();
    return {ok: true, data: data as T};
  } catch {
    return {ok: false, message: 'Couldn’t reach the server. Please try again.', unauthenticated: false};
  }
}
