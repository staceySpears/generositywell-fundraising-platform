import { useState } from 'react';
import { PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import styles from './CampaignDetailPage.module.css';

const formatDollars = (cents) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format((cents ?? 0) / 100);

/**
 * Card entry form rendered inside a Stripe <Elements> provider.
 * Calls stripe.confirmPayment() with redirect:'if_required' so standard
 * card payments complete in-place without a page redirect.
 *
 * @param {{ amountInCents: number, onSuccess: () => void, onBack: () => void }} props
 */
export default function StripePaymentForm({ amountInCents, onSuccess, onBack }) {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setIsProcessing(true);
    setErrorMessage(null);

    const { error } = await stripe.confirmPayment({
      elements,
      confirmParams: {
        // Required by Stripe even when redirect:'if_required' avoids an actual redirect
        return_url: window.location.href,
      },
      redirect: 'if_required',
    });

    if (error) {
      // card_error and validation_error messages are safe to show to the user
      setErrorMessage(error.message ?? 'Payment failed. Please try again.');
      setIsProcessing(false);
    } else {
      // Payment succeeded — the webhook will record the donation asynchronously
      onSuccess();
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className={styles.donateForm}>
      <h2 className={styles.donateTitle}>Pay {formatDollars(amountInCents)}</h2>

      <div className={styles.stripeElement}>
        <PaymentElement />
      </div>

      {errorMessage && <p className={styles.serverError}>{errorMessage}</p>}

      <button type="submit" className={styles.donateBtn} disabled={isProcessing || !stripe}>
        {isProcessing ? 'Processing…' : `Donate ${formatDollars(amountInCents)}`}
      </button>

      <button type="button" className={styles.backBtn} onClick={onBack} disabled={isProcessing}>
        ← Change amount
      </button>
    </form>
  );
}
