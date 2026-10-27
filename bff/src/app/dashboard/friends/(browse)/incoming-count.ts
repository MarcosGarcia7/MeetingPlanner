import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';

// "3", or "20+" when the first page of incoming requests isn't the last one; null when there are none.
export function incomingCountLabel(page: CursorPage<FriendshipResponse>): string | null {
  if (page.items.length === 0) {
    return null;
  }
  return page.nextCursor ? `${page.items.length}+` : String(page.items.length);
}
