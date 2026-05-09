import { useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as Label from '@radix-ui/react-label';
import { QRCodeSVG } from 'qrcode.react';
import { Elements } from '@stripe/react-stripe-js';
import { getCampaignById, createPaymentIntent } from '../../api/campaignApi.js';
import { stripePromise } from '../../lib/stripe.js';
import { donationSchema } from '../../schemas/campaign.js';
import StripePaymentForm from './StripePaymentForm.jsx';
import styles from './CampaignDetailPage.module.css';

/** Formats an ISO date string (e.g. "2026-05-01") as "May 1, 2026". */
const formatDate = (iso) => {
  if (!iso) return '';
  // Append T00:00 so Date parses as local time, not UTC midnight (which shifts by timezone).
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(new Date(`${iso}T00:00`));
};

const formatDollars = (cents) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format((cents ?? 0) / 100);

const progressPercent = (current, goal) =>
  goal > 0 ? Math.min(100, Math.round(((current ?? 0) / goal) * 100)) : 0;

/** Downloads the campaign QR code as a standalone SVG file for printing. */
function downloadQR(qrRef, campaignName) {
  const svg = qrRef.current?.querySelector('svg');
  if (!svg) return;
  const serialized = new XMLSerializer().serializeToString(svg);
  const blob = new Blob([serialized], { type: 'image/svg+xml' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `${campaignName.replace(/\s+/g, '-').toLowerCase()}-qr.svg`;
  anchor.click();
  URL.revokeObjectURL(url);
}

/**
 * Donation flow states:
 *   'amount'  — user enters dollar amount
 *   'card'    — Stripe PaymentElement shown for card entry
 *   'success' — payment confirmed; webhook records the donation asynchronously
 */

export default function CampaignDetailPage() {
  const { id } = useParams();
  const qrRef = useRef(null);

  // Donation flow state
  const [paymentStep, setPaymentStep] = useState('amount');
  const [pendingPayment, setPendingPayment] = useState(null); // { clientSecret, amountInCents }

  const {
    data: campaign,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['campaigns', id],
    queryFn: () => getCampaignById(id),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({ resolver: zodResolver(donationSchema) });

  // Step 1: create a PaymentIntent on the backend, then advance to the card step
  const createIntentMutation = useMutation({
    mutationFn: ({ dollars }) => createPaymentIntent(id, Math.round(dollars * 100)),
    onSuccess: (data, variables) => {
      setPendingPayment({
        clientSecret: data.clientSecret,
        amountInCents: Math.round(variables.dollars * 100),
      });
      setPaymentStep('card');
      reset();
    },
  });

  const handlePaymentSuccess = () => {
    setPaymentStep('success');
    setPendingPayment(null);
  };

  const handleBackToAmount = () => {
    setPaymentStep('amount');
    setPendingPayment(null);
    createIntentMutation.reset();
  };

  if (isLoading) return <p className={styles.state}>Loading…</p>;
  if (isError) return <p className={styles.stateError}>Campaign not found.</p>;

  const pct = progressPercent(campaign.currentAmount, campaign.goalAmount);
  const isClosed = campaign.status === 'CLOSED';

  return (
    <div className={styles.layout}>
      <section className={styles.main}>
        <div className={styles.meta}>
          <span className={styles.category}>{campaign.category}</span>
          <span className={`${styles.status} ${styles[campaign.status?.toLowerCase()]}`}>
            {campaign.status}
          </span>
        </div>

        <h1 className={styles.title}>{campaign.name}</h1>
        {campaign.user?.name && (
          <p className={styles.organizer}>Organized by {campaign.user.name}</p>
        )}
        <p className={styles.description}>{campaign.description}</p>

        {campaign.date && campaign.deadline && (
          <p className={styles.dates}>
            {formatDate(campaign.date)} → {formatDate(campaign.deadline)}
          </p>
        )}
      </section>

      <aside className={styles.sidebar}>
        <div className={styles.progressCard}>
          <p className={styles.raised}>{formatDollars(campaign.currentAmount)}</p>
          <p className={styles.goal}>raised of {formatDollars(campaign.goalAmount)} goal</p>
          <div className={styles.progressBar}>
            <div className={styles.progressFill} style={{ width: `${pct}%` }} />
          </div>
          <p className={styles.pct}>{pct}% funded</p>
        </div>

        {!isClosed && (
          <>
            {/* ── Step 1: amount entry ──────────────────── */}
            {paymentStep === 'amount' && (
              <form
                onSubmit={handleSubmit((values) => createIntentMutation.mutate(values))}
                noValidate
                className={styles.donateForm}
              >
                <h2 className={styles.donateTitle}>Make a donation</h2>
                <div className={styles.field}>
                  <Label.Root htmlFor="dollars" className={styles.label}>
                    Amount (USD)
                  </Label.Root>
                  <div className={styles.inputWrapper}>
                    <span className={styles.currency}>$</span>
                    <input
                      id="dollars"
                      type="number"
                      min="1"
                      step="1"
                      placeholder="25"
                      className={`${styles.input} ${errors.dollars ? styles.inputError : ''}`}
                      {...register('dollars', { valueAsNumber: true })}
                    />
                  </div>
                  {errors.dollars && <span className={styles.error}>{errors.dollars.message}</span>}
                </div>

                {createIntentMutation.isError && (
                  <p className={styles.serverError}>
                    {createIntentMutation.error?.response?.data?.message ??
                      'Could not start payment. Please try again.'}
                  </p>
                )}

                <button
                  type="submit"
                  className={styles.donateBtn}
                  disabled={createIntentMutation.isPending}
                >
                  {createIntentMutation.isPending ? 'Preparing…' : 'Continue to payment →'}
                </button>
              </form>
            )}

            {/* ── Step 2: card entry via Stripe ─────────── */}
            {paymentStep === 'card' && pendingPayment && (
              <Elements
                stripe={stripePromise}
                options={{ clientSecret: pendingPayment.clientSecret }}
              >
                <StripePaymentForm
                  amountInCents={pendingPayment.amountInCents}
                  onSuccess={handlePaymentSuccess}
                  onBack={handleBackToAmount}
                />
              </Elements>
            )}

            {/* ── Step 3: success ───────────────────────── */}
            {paymentStep === 'success' && (
              <div className={styles.successCard}>
                <p className={styles.successHeading}>🎉 Thank you!</p>
                <p className={styles.successNote}>
                  Your payment is confirmed. The campaign total will update shortly as your donation
                  is processed.
                </p>
                <button
                  type="button"
                  className={styles.backBtn}
                  onClick={() => setPaymentStep('amount')}
                >
                  Donate again
                </button>
              </div>
            )}
          </>
        )}

        {isClosed && (
          <p className={styles.closedNotice}>
            This campaign has closed. Thank you to all who contributed!
          </p>
        )}

        <div className={styles.qrCard} ref={qrRef}>
          <p className={styles.qrTitle}>Share this campaign</p>
          <div className={styles.qrCode}>
            <QRCodeSVG
              value={`${window.location.origin}/campaigns/${campaign.id}`}
              size={180}
              marginSize={1}
            />
          </div>
          <button
            type="button"
            className={styles.qrDownloadBtn}
            onClick={() => downloadQR(qrRef, campaign.name)}
          >
            Download for flyers
          </button>
        </div>
      </aside>
    </div>
  );
}
