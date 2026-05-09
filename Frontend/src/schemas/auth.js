import { z } from 'zod';

/**
 * Mirrors the Spring Boot Bean Validation constraints on LoginRequest.java.
 * Source of truth: Application/.../controller/model/LoginRequest.java
 */
export const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

/**
 * Mirrors the Bean Validation constraints on CreateUserRequest.java.
 * Source of truth: Application/.../controller/model/CreateUserRequest.java
 *
 * NOTE: if you tighten the backend password rule (e.g. @Size(min=8)),
 * update the min() here to match.
 *
 * These schemas are intentionally in a standalone module so the future
 * React Native app can import them without pulling in page-level code.
 */
export const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});
