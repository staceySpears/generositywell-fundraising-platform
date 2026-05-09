import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAllEvents } from '../../api/eventApi.js';
import styles from './CalendarPage.module.css';

const monthLabel = (iso) =>
  new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(
    new Date(iso + 'T12:00:00')
  );

const dayLabel = (iso) =>
  new Intl.DateTimeFormat('en-US', { weekday: 'short', month: 'short', day: 'numeric' }).format(
    new Date(iso + 'T12:00:00')
  );

const STATUS_DOT = {
  PLANNING: styles.dotPlanning,
  SCHEDULED: styles.dotScheduled,
  IN_PROGRESS: styles.dotInProgress,
  COMPLETED: styles.dotCompleted,
  CANCELLED: styles.dotCancelled,
};

/** Group an array by a string key derived from each item. */
function groupBy(items, keyFn) {
  return items.reduce((acc, item) => {
    const key = keyFn(item);
    if (!acc[key]) acc[key] = [];
    acc[key].push(item);
    return acc;
  }, {});
}

export default function CalendarPage() {
  const {
    data: events,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['events'],
    queryFn: getAllEvents,
  });

  if (isLoading) return <p className={styles.state}>Loading calendar…</p>;
  if (isError) return <p className={styles.stateError}>Failed to load events. Please try again.</p>;

  const active = (events ?? []).filter((e) => e.status !== 'CANCELLED');

  if (!active.length)
    return (
      <div>
        <h1 className={styles.heading}>Calendar</h1>
        <p className={styles.state}>No upcoming events scheduled yet.</p>
      </div>
    );

  // Sort by eventDate ascending, then group by "Month Year"
  const sorted = [...active].sort((a, b) => (a.eventDate ?? '').localeCompare(b.eventDate ?? ''));
  const grouped = groupBy(sorted, (e) => (e.eventDate ? monthLabel(e.eventDate) : 'Undated'));

  return (
    <div>
      <h1 className={styles.heading}>Calendar</h1>

      {Object.entries(grouped).map(([month, monthEvents]) => (
        <section key={month} className={styles.month}>
          <h2 className={styles.monthHeading}>{month}</h2>
          <ul className={styles.list}>
            {monthEvents.map((event) => (
              <li key={event.id} className={styles.item}>
                <div className={styles.dateBadge}>
                  {event.eventDate ? dayLabel(event.eventDate) : '—'}
                </div>
                <div className={styles.itemBody}>
                  <div className={styles.itemHeader}>
                    <span className={styles.eventName}>{event.name}</span>
                    <span className={`${styles.dot} ${STATUS_DOT[event.status] ?? ''}`} />
                  </div>
                  {event.location && <span className={styles.location}>{event.location}</span>}
                  <div className={styles.itemFooter}>
                    <span className={styles.rsvpCount}>
                      {event.confirmedCount} confirmed
                      {event.waitlistedCount > 0 && ` · ${event.waitlistedCount} waitlisted`}
                    </span>
                    {event.status === 'SCHEDULED' && (
                      <Link to="/events" className={styles.rsvpLink}>
                        RSVP →
                      </Link>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
