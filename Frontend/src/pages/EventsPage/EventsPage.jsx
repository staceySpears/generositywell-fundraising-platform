import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getAllEvents, rsvpToEvent, cancelRsvp } from '../../api/eventApi.js';
import { getUserById } from '../../api/userApi.js';
import { useAuth } from '../../hooks/useAuth.js';
import styles from './EventsPage.module.css';

const STATUS_LABEL = {
  PLANNING: 'Planning',
  SCHEDULED: 'Open for RSVPs',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const formatDate = (iso) =>
  iso
    ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(
        new Date(iso + 'T12:00:00')
      )
    : '—';

export default function EventsPage() {
  const { isAuthenticated, userId } = useAuth();
  const queryClient = useQueryClient();
  const [rsvpError, setRsvpError] = useState({});

  const {
    data: events,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['events'],
    queryFn: getAllEvents,
  });

  // Fetch profile so RSVPs carry the user's real name and email.
  // Only runs when the user is logged in; disabled for anonymous visitors.
  // isProfileLoading is used to keep the RSVP button disabled until the
  // profile resolves — avoids submitting empty name/email before the fetch completes.
  const { data: userProfile, isLoading: isProfileLoading } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => getUserById(userId),
    enabled: isAuthenticated && !!userId,
  });

  const rsvpMutation = useMutation({
    mutationFn: ({ eventId, payload }) => rsvpToEvent(eventId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }),
    onError: (err, variables) => {
      const msg = err.response?.data?.message ?? 'Failed to RSVP. Please try again.';
      setRsvpError((prev) => ({ ...prev, [variables.eventId]: msg }));
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ eventId, volunteerId }) => cancelRsvp(eventId, volunteerId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }),
    onError: (err, variables) => {
      const msg = err.response?.data?.message ?? 'Failed to cancel RSVP. Please try again.';
      setRsvpError((prev) => ({ ...prev, [variables.eventId]: msg }));
    },
  });

  if (isLoading) return <p className={styles.state}>Loading events…</p>;
  if (isError) return <p className={styles.stateError}>Failed to load events. Please try again.</p>;
  if (!events?.length)
    return (
      <div>
        <h1 className={styles.heading}>Volunteer Events</h1>
        <p className={styles.state}>No events scheduled yet.</p>
      </div>
    );

  const scheduled = events.filter((e) => e.status === 'SCHEDULED');
  const other = events.filter((e) => e.status !== 'SCHEDULED');

  const myRsvp = (event) =>
    event.volunteers?.find((v) => v.id === userId && v.rsvpStatus !== 'CANCELLED');

  const handleRsvp = (event) => {
    setRsvpError((prev) => ({ ...prev, [event.id]: null }));
    rsvpMutation.mutate({
      eventId: event.id,
      payload: {
        volunteerId: userId,
        volunteerName: userProfile?.name ?? '',
        volunteerEmail: userProfile?.email ?? '',
      },
    });
  };

  const handleCancelRsvp = (event) => {
    setRsvpError((prev) => ({ ...prev, [event.id]: null }));
    cancelMutation.mutate({ eventId: event.id, volunteerId: userId });
  };

  const renderEvent = (event) => {
    const existing = myRsvp(event);
    const isScheduled = event.status === 'SCHEDULED';
    // Also block the RSVP button while the user profile is still loading so
    // we never submit an empty volunteerName / volunteerEmail to the backend.
    const isBusy =
      (isAuthenticated && isProfileLoading) ||
      (rsvpMutation.isPending && rsvpMutation.variables?.eventId === event.id) ||
      (cancelMutation.isPending && cancelMutation.variables?.eventId === event.id);

    return (
      <li key={event.id} className={styles.card}>
        <div className={styles.cardHeader}>
          <span className={`${styles.status} ${styles[event.status?.toLowerCase()]}`}>
            {STATUS_LABEL[event.status] ?? event.status}
          </span>
          {event.spotsRemaining != null && (
            <span className={styles.spots}>
              {event.spotsRemaining === 0
                ? 'Fully booked'
                : `${event.spotsRemaining} spot${event.spotsRemaining === 1 ? '' : 's'} left`}
            </span>
          )}
        </div>

        <h2 className={styles.cardTitle}>{event.name}</h2>
        {event.description && <p className={styles.cardDesc}>{event.description}</p>}

        <dl className={styles.meta}>
          {event.location && (
            <>
              <dt>Location</dt>
              <dd>{event.location}</dd>
            </>
          )}
          <dt>Date</dt>
          <dd>{formatDate(event.eventDate)}</dd>
          <dt>RSVP by</dt>
          <dd>{formatDate(event.registrationDeadline)}</dd>
          <dt>Volunteers</dt>
          <dd>
            {event.confirmedCount} confirmed
            {event.waitlistedCount > 0 && ` · ${event.waitlistedCount} waitlisted`}
          </dd>
        </dl>

        {rsvpError[event.id] && <p className={styles.error}>{rsvpError[event.id]}</p>}

        {isAuthenticated && isScheduled && (
          <div className={styles.actions}>
            {existing ? (
              <>
                <span className={styles.rsvpBadge}>
                  {existing.rsvpStatus === 'CONFIRMED' ? '✓ Confirmed' : '⏳ Waitlisted'}
                </span>
                <button
                  className={styles.cancelBtn}
                  onClick={() => handleCancelRsvp(event)}
                  disabled={isBusy}
                >
                  {isBusy ? 'Cancelling…' : 'Cancel RSVP'}
                </button>
              </>
            ) : (
              <button
                className={styles.rsvpBtn}
                onClick={() => handleRsvp(event)}
                disabled={isBusy}
              >
                {isBusy ? 'Submitting…' : 'RSVP'}
              </button>
            )}
          </div>
        )}

        {!isAuthenticated && isScheduled && (
          <p className={styles.signInPrompt}>
            <a href="/login">Sign in</a> to RSVP for this event.
          </p>
        )}
      </li>
    );
  };

  return (
    <div>
      <h1 className={styles.heading}>Volunteer Events</h1>

      {scheduled.length > 0 && (
        <section>
          <h2 className={styles.sectionHeading}>Open for RSVPs</h2>
          <ul className={styles.list}>{scheduled.map(renderEvent)}</ul>
        </section>
      )}

      {other.length > 0 && (
        <section className={styles.otherSection}>
          <h2 className={styles.sectionHeading}>All Events</h2>
          <ul className={styles.list}>{other.map(renderEvent)}</ul>
        </section>
      )}
    </div>
  );
}
