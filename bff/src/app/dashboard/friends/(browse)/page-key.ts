import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';

// A React key for a cursor list seeded from `page`: it changes whenever the server's first page does.
export function pageKey(page: CursorPage<FriendshipResponse> | null): string {
  return page ? `${page.items.map((item) => item.userId).join(',')}|${page.nextCursor ?? ''}` : 'failed';
}
