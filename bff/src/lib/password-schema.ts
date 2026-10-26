import {z} from 'zod';

// Mirrors the server's password rules (12–72 characters, at most 72 bytes for bcrypt).
export const passwordSchema = z
  .string()
  .min(12, 'Password must be at least 12 characters')
  .max(72, 'Password cannot exceed 72 characters')
  // bcrypt's limit is 72 bytes, so accented letters and emoji count for more than one.
  .refine(
    (password) => new TextEncoder().encode(password).length <= 72,
    'Password is too long; accented letters and emoji count as more than one character'
  );
