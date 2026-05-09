import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';
import { getUserById, getUserCampaigns, getUserEvents } from '../../api/userApi.js';
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

export default function DashboardPage() {
  const { userId } = useAuth();

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

  if (userLoading) return <p className={styles.state}>Loading your dashboard…</p>;

  return (
    <div className={styles.container}>
      <h1 className={styles.heading}>
        Welcome back{user?.name ? `, ${user.name.split(' ')[0]}` : ''}!
      </h1>

      <div className={styles.grid}>
        {/* Profile card */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Your profile</h2>
          {user && (
            <dl className={styles.dl}>
              <dt>Name</dt>
              <dd>{user.name}</dd>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </dl>
          )}
        </div>

        {/* My Campaigns card */}
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

        {/* My Events card */}
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

        {/* Quick links card */}
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

        {/* Giving history — Phase 4 */}
        <div className={`${styles.card} ${styles.comingSoon}`}>
          <h2 className={styles.cardTitle}>Giving history</h2>
          <p className={styles.placeholder}>
            Your donation and volunteer history will appear here. (Phase 4)
          </p>
        </div>
      </div>
    </div>
  );
}
