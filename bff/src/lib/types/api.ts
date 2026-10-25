// Error body every API (and BFF) error response carries. Branch on `error`, show `message`.
export interface ApiErrorBody {
  error: string;
  message: string;
}

// Mirrors server common/api/dto/CursorPage.
export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}
