import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAllCampaigns } from '../../api/campaignApi.js';
import styles from './SearchPage.module.css';

export default function SearchPage() {
  const [query, setQuery] = useState('');

  const { data: campaigns = [], isLoading, isError } = useQuery({
    queryKey: ['campaigns'],
    queryFn: getAllCampaigns,
  });

  const filtered = campaigns.filter((c) => {
    const q = query.toLowerCase();
    return (
      c.name?.toLowerCase().includes(q) ||
      c.description?.toLowerCase().includes(q) ||
      c.category?.toLowerCase().includes(q)
    );
  });

  return (
    <div>
      <h1 className={styles.heading}>Search campaigns</h1>
      <input
        type="search"
        className={styles.searchInput}
        placeholder="Search by name, description, or category…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoFocus
      />

      {isLoading && <p className={styles.state}>Loading…</p>}
      {isError   && <p className={styles.stateError}>Failed to load campaigns.</p>}

      {!isLoading && !isError && (
        <p className={styles.resultCount}>
          {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          {query ? ` for "${query}"` : ''}
        </p>
      )}

      <ul className={styles.list}>
        {filtered.map((c) => (
          <li key={c.id}>
            <Link to={`/campaigns/${c.id}`} className={styles.item}>
              <div className={styles.itemHeader}>
                <span className={styles.itemName}>{c.name}</span>
                <span className={styles.category}>{c.category}</span>
              </div>
              <p className={styles.itemDesc}>{c.description}</p>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
