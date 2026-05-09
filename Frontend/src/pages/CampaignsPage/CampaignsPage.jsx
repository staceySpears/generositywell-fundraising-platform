import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAllCampaigns } from '../../api/campaignApi.js';
import styles from './CampaignsPage.module.css';

/** Formats cents as a dollar string, e.g. 150000 → "$1,500" */
const formatDollars = (cents) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format((cents ?? 0) / 100);

const progressPercent = (current, goal) =>
  goal > 0 ? Math.min(100, Math.round(((current ?? 0) / goal) * 100)) : 0;

export default function CampaignsPage() {
  const {
    data: campaigns,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['campaigns'],
    queryFn: getAllCampaigns,
  });

  if (isLoading) return <p className={styles.state}>Loading campaigns…</p>;
  if (isError)
    return <p className={styles.stateError}>Failed to load campaigns. Please try again.</p>;
  if (!campaigns?.length)
    return (
      <p className={styles.state}>
        No campaigns yet. <Link to="/register">Start one!</Link>
      </p>
    );

  return (
    <div>
      <h1 className={styles.heading}>Active campaigns</h1>
      <div className={styles.grid}>
        {campaigns.map((c) => {
          const pct = progressPercent(c.currentAmount, c.goalAmount);
          return (
            <Link key={c.id} to={`/campaigns/${c.id}`} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={styles.category}>{c.category}</span>
                <span className={`${styles.status} ${styles[c.status?.toLowerCase()]}`}>
                  {c.status}
                </span>
              </div>
              <h2 className={styles.cardTitle}>{c.name}</h2>
              <p className={styles.cardDesc}>{c.description}</p>
              <div className={styles.progressBar}>
                <div className={styles.progressFill} style={{ width: `${pct}%` }} />
              </div>
              <div className={styles.amounts}>
                <span>{formatDollars(c.currentAmount)} raised</span>
                <span>
                  {pct}% of {formatDollars(c.goalAmount)}
                </span>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
