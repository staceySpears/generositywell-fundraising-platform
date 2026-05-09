import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { getUserById } from '../../api/userApi.js';
import styles from './DashboardPage.module.css';

export default function DashboardPage() {
  const { userId } = useAuth();

  const { data: user, isLoading, isError } = useQuery({
    queryKey: ['users', userId],
    queryFn: () => getUserById(userId),
    enabled: !!userId,
  });

  if (isLoading) return <p className={styles.state}>Loading your dashboard…</p>;
  if (isError)   return <p className={styles.stateError}>Failed to load profile.</p>;

  return (
    <div className={styles.container}>
      <h1 className={styles.heading}>Welcome back{user?.name ? `, ${user.name.split(' ')[0]}` : ''}!</h1>

      <div className={styles.grid}>
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Your profile</h2>
          {user && (
            <dl className={styles.dl}>
              <dt>Name</dt>  <dd>{user.name}</dd>
              <dt>Email</dt> <dd>{user.email}</dd>
            </dl>
          )}
        </div>

        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Quick links</h2>
          <ul className={styles.quickLinks}>
            <li><Link to="/campaigns">Browse campaigns</Link></li>
            <li><Link to="/events">Upcoming events</Link></li>
            <li><Link to="/calendar">My calendar</Link></li>
          </ul>
        </div>

        <div className={`${styles.card} ${styles.comingSoon}`}>
          <h2 className={styles.cardTitle}>Giving history</h2>
          <p className={styles.placeholder}>Your donation and volunteer history will appear here. (Phase 4)</p>
        </div>
      </div>
    </div>
  );
}
