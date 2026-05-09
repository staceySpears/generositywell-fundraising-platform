import { loadStripe } from '@stripe/stripe-js';

/**
 * Singleton Stripe.js promise.
 * loadStripe() must be called outside of a component render to avoid
 * re-creating the Stripe object on every render.
 * Set VITE_STRIPE_PUBLISHABLE_KEY in .env.local (see .env.example).
 */
export const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? '');
