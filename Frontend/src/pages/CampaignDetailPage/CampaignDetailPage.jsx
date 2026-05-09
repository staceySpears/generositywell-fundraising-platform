import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as Label from '@radix-ui/react-label';
import { getCampaignById, donate } from '../../api/campaignApi.js';
import styles from './CampaignDetailPage.module.css';

const donationSchema = z.object({
  dollars: z
    .number({ invalid_type_error: 'Enter an amount' })
    .positive('Amount must be greater than $0')
    .max(100_000, 'Maximum single donation is $100,000'),
});

const formatDollars = (cents) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(
    (cents ?? 0) / 100
  );

const progressPercent = (current, goal) =>
  goal > 0 ? Math.min(100, Math.round(((current ?? 0) / goal) * 100)) : 0;

export default function CampaignDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data: campaign, isLoading, isError } = useQuery({
    queryKey: ['campaigns', id],
    queryFn: () => getCampaignById(id),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({ resolver: zodResolver(donationSchema) });

  const mutation = useMutation({
    mutationFn: ({ dollars }) => donate(id, Math.round(dollars * 100)),
    onSuccess: () => {
      // Invalidate both the detail and the list so both reflect the new total.
      queryClient.invalidateQueries({ queryKey: ['campaigns', id] });
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      reset();
    },
  });

  if (isLoading) return <p className={styles.state}>Loading…</p>;
  if (isError)   return <p className={styles.stateError}>Campaign not found.</p>;

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
            {campaign.date} → {campaign.deadline}
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
          <form
            onSubmit={handleSubmit((values) => mutation.mutate(values))}
            noValidate
            className={styles.donateForm}
          >
            <h2 className={styles.donateTitle}>Make a donation</h2>
            <div className={styles.field}>
              <Label.Root htmlFor="dollars" className={styles.label}>Amount (USD)</Label.Root>
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

            {mutation.isError && (
              <p className={styles.serverError}>
                {mutation.error?.response?.data?.message ?? 'Donation failed. Please try again.'}
              </p>
            )}
            {mutation.isSuccess && (
              <p className={styles.serverSuccess}>Thank you for your donation!</p>
            )}

            <button
              type="submit"
              className={styles.donateBtn}
              disabled={mutation.isPending}
            >
              {mutation.isPending ? 'Processing…' : 'Donate'}
            </button>
          </form>
        )}

        {isClosed && (
          <p className={styles.closedNotice}>This campaign has closed. Thank you to all who contributed!</p>
        )}
      </aside>
    </div>
  );
}
