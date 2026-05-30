import { loadStripe } from '@stripe/stripe-js';

/**
 * Singleton Stripe.js promise.
 * loadStripe() must be called outside of a component render to avoid
 * re-creating the Stripe object on every render.
 * Set VITE_STRIPE_PUBLISHABLE_KEY in .env.local (see .env.example).
 *
 * Resolves to null when the key is absent so the rest of the app loads
 * normally; <Elements stripe={null}> renders a loading state rather than
 * crashing. A console warning makes the missing config visible in dev.
 */
const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;

if (!stripeKey) {
  console.warn(
    '[GenerosityWell] VITE_STRIPE_PUBLISHABLE_KEY is not set. ' +
      'Payment features will not work until you add it to .env.local — see .env.example.'
  );
}

export const stripePromise = stripeKey ? loadStripe(stripeKey) : null;
