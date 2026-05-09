import { z } from 'zod';

/**
 * Mirrors the Bean Validation constraints on DonationRequest.java.
 * Source of truth: Application/.../controller/model/DonationRequest.java
 *
 * The UI collects whole-dollar amounts; the API expects cents.
 * Conversion: Math.round(dollars * 100) happens at the call site.
 */
export const donationSchema = z.object({
  dollars: z
    .number({ invalid_type_error: 'Enter an amount' })
    .int('Whole dollars only — no cents')
    .positive('Amount must be greater than $0')
    .max(100_000, 'Maximum single donation is $100,000'),
});
