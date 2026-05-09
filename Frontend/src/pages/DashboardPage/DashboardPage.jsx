import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import * as Label from '@radix-ui/react-label';
import { useAuth } from '../../hooks/useAuth.js';
import {
  getUserById,
  getUserCampaigns,
  getUserEvents,
  getUserDonations,
  getUserRsvps,
  updateUser,
} from '../../api/userApi.js';
import { profileEditSchema } from '../../schemas/auth.js';
import styles from './DashboardPage.module.css';

const formatDate = (iso) =>
  iso
    ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(
        new Date(`${iso}T00:00`)
      )
    : null;

const formatDollars = (cents) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format((cents ?? 0) / 100);

const RSVP_LABEL = { CONFIRMED: '✓ Confirmed', WAITLISTED: '⏳ Waitlisted' };

export default function DashboardPage() {
  const { userId } = useAuth();
  const queryClient = useQueryClient();
  const [editingProfile, setEditingProfile] = useState(false);

  /* ── Queries ──────────────────────────────────────────────── */
  const { data: user, isLoading: userLoading } = useQuery({
    queryKey: ['users', userId],
    queryFn: () => getUserById(userId),
    enabled: !!userId,
  });

  const { data: campaigns = [], isLoading: campaignsLoading } = useQuery({
    queryKey: ['users', userId, 'campaigns'],
    queryFn: () => getUserCampaigns(userId),
    enabled: !!userId,
  });

  const { data: events = [], isLoading: eventsLoading } = useQuery({
    queryKey: ['users', userId, 'events'],
    queryFn: () => getUserEvents(userId),
    enabled: !!userId,
  });

  const { data: donations = [], isLoading: donationsLoading } = useQuery({
    queryKey: ['users', userId, 'donations'],
    queryFn: () => getUserDonations(userId),
    enabled: !!userId,
  });

  const { data: rsvps = [], isLoading: rsvpsLoading } = useQuery({
    queryKey: ['users', userId, 'rsvps'],
    queryFn: () => getUserRsvps(userId),
    enabled: !!userId,
  });

  /* ── Profile edit form ────────────────────────────────────── */
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(profileEditSchema),
    values: user ? { name: user.name, email: user.email } : undefined,
  });

  const profileMutation = useMutation({
    mutationFn: (values) => updateUser({ id: userId, ...values }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', userId] });
      setEditingProfile(false);
    },
  });

  const cancelEdit = () => {
    reset();
    setEditingProfile(false);
  };

  /* ── Render helpers ───────────────────────────────────────── */
  const myRsvpStatus = (event) => {
    const entry = event.volunteers?.find((v) => v.id === userId && v.rsvpStatus !== 'CANCELLED');
    return entry?.rsvpStatus ?? null;
  };

  if (userLoading) return <p className={styles.state}>Loading your dashboard…</p>;

  return (
    <div className={styles.container}>
      <h1 className={styles.heading}>
        Welcome back{user?.name ? `, ${user.name.split(' ')[0]}` : ''}!
      </h1>

      <div className={styles.grid}>
        {/* ── Profile card ──────────────────────────────── */}
        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}>Your profile</h2>
            {!editingProfile && (
              <button className={styles.editBtn} onClick={() => setEditingProfile(true)}>
                Edit
              </button>
            )}
          </div>

          {editingProfile ? (
            <form
              onSubmit={handleSubmit((values) => profileMutation.mutate(values))}
              noValidate
              className={styles.editForm}
            >
              <div className={styles.field}>
                <Label.Root htmlFor="prof-name" className={styles.label}>
                  Name
                </Label.Root>
                <input
                  id="prof-name"
                  type="text"
                  className={`${styles.input} ${errors.name ? styles.inputError : ''}`}
                  {...register('name')}
                />
                {errors.name && <span className={styles.fieldError}>{errors.name.message}</span>}
              </div>

              <div className={styles.field}>
                <Label.Root htmlFor="prof-email" className={styles.label}>
                  Email
                </Label.Root>
                <input
                  id="prof-email"
                  type="email"
                  className={`${styles.input} ${errors.email ? styles.inputError : ''}`}
                  {...register('email')}
                />
                {errors.email && <span className={styles.fieldError}>{errors.email.message}</span>}
              </div>

              {profileMutation.isError && (
                <p className={styles.serverError}>Failed to save. Please try again.</p>
              )}

              <div className={styles.editActions}>
                <button type="submit" className={styles.saveBtn} disabled={isSubmitting}>
                  {isSubmitting ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className={styles.cancelBtn} onClick={cancelEdit}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            user && (
              <dl className={styles.dl}>
                <dt>Name</dt>
                <dd>{user.name}</dd>
                <dt>Email</dt>
                <dd>{user.email}</dd>
              </dl>
            )
          )}
        </div>

        {/* ── My Campaigns ──────────────────────────────── */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>My campaigns</h2>
          {campaignsLoading ? (
            <p className={styles.placeholder}>Loading…</p>
          ) : campaigns.length === 0 ? (
            <p className={styles.placeholder}>
              You haven&apos;t created any campaigns yet.{' '}
              <Link to="/campaigns">Browse campaigns</Link> for inspiration.
            </p>
          ) : (
            <ul className={styles.itemList}>
              {campaigns.map((c) => (
                <li key={c.id} className={styles.item}>
                  <Link to={`/campaigns/${c.id}`} className={styles.itemName}>
                    {c.name}
                  </Link>
                  <span className={styles.itemMeta}>
                    {formatDollars(c.currentAmount)} raised
                    {c.deadline ? ` · ends ${formatDate(c.deadline)}` : ''}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* ── My Events (organized) ─────────────────────── */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>My events</h2>
          {eventsLoading ? (
            <p className={styles.placeholder}>Loading…</p>
          ) : events.length === 0 ? (
            <p className={styles.placeholder}>
              You haven&apos;t organized any events yet.{' '}
              <Link to="/events">See upcoming events</Link>.
            </p>
          ) : (
            <ul className={styles.itemList}>
              {events.map((e) => (
                <li key={e.id} className={styles.item}>
                  <span className={styles.itemName}>{e.name}</span>
                  <span className={styles.itemMeta}>
                    {e.eventDate ? formatDate(e.eventDate) : 'Date TBD'}
                    {e.location ? ` · ${e.location}` : ''}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* ── My RSVPs ──────────────────────────────────── */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>My RSVPs</h2>
          {rsvpsLoading ? (
            <p className={styles.placeholder}>Loading…</p>
          ) : rsvps.length === 0 ? (
            <p className={styles.placeholder}>
              No upcoming RSVPs. <Link to="/events">Browse volunteer events</Link>.
            </p>
          ) : (
            <ul className={styles.itemList}>
              {rsvps.map((e) => {
                const status = myRsvpStatus(e);
                return (
                  <li key={e.id} className={styles.item}>
                    <span className={styles.itemName}>{e.name}</span>
                    <span className={styles.itemMeta}>
                      {e.eventDate ? formatDate(e.eventDate) : 'Date TBD'}
                      {status && ` · ${RSVP_LABEL[status] ?? status}`}
                    </span>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* ── Giving history ────────────────────────────── */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Giving history</h2>
          {donationsLoading ? (
            <p className={styles.placeholder}>Loading…</p>
          ) : donations.length === 0 ? (
            <p className={styles.placeholder}>
              Your donations will appear here after you contribute to a campaign.
            </p>
          ) : (
            <ul className={styles.itemList}>
              {donations.map((d) => (
                <li key={d.campaignId} className={styles.item}>
                  <Link to={`/campaigns/${d.campaignId}`} className={styles.itemName}>
                    {d.campaignName}
                  </Link>
                  <span className={styles.itemMeta}>
                    {d.amountInCents != null
                      ? formatDollars(d.amountInCents)
                      : 'Amount not tracked'}
                    {d.donationDate ? ` · ${formatDate(d.donationDate)}` : ''}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* ── Quick links ───────────────────────────────── */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Quick links</h2>
          <ul className={styles.quickLinks}>
            <li>
              <Link to="/campaigns">Browse campaigns</Link>
            </li>
            <li>
              <Link to="/events">Upcoming events</Link>
            </li>
            <li>
              <Link to="/calendar">My calendar</Link>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
