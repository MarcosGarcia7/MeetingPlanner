import type {ApiErrorBody} from './types/api';

// Reads the { error, message } body of a failed /api response, or null if it isn't JSON.
export async function readApiError(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}
