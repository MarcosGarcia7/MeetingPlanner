import {Style, Avatar} from '@dicebear/core';
import initialsDefinition from '@dicebear/styles/initials.json' with {type: 'json'};

export interface AvatarFields {
  username: string;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string;
}

const style = new Style(initialsDefinition);

// Initials avatar: "First Last" when both names are set, otherwise the username. Shared by the
// /api/avatar/me route and client-side previews so both render the same image.
export function avatarSvg({username, firstName, lastName, avatarColor}: AvatarFields): string {
  const name = firstName && lastName ? `${firstName} ${lastName}` : username;
  return new Avatar(style, {seed: name, borderRadius: 50, backgroundColor: [avatarColor]}).toString();
}

export function avatarDataUri(fields: AvatarFields): string {
  return `data:image/svg+xml;utf8,${encodeURIComponent(avatarSvg(fields))}`;
}
