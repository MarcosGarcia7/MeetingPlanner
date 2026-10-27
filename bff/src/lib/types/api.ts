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

// Mirrors server common/api/model/TargetKind: the `kind` of an invite or join request, and the
// `?kind=` filter on GET /api/invites and GET /api/requests.
export type TargetKind = 'GROUP' | 'MEETING';
