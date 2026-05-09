import styles from './EventsPage.module.css';

/**
 * Placeholder — FundraisingEvent entity is Phase 3 backend work.
 * Once the /events endpoints exist, this page will use TanStack Query
 * to fetch and display events the authenticated user is registered for.
 */
export default function EventsPage() {
  return (
    <div>
      <h1 className={styles.heading}>Events</h1>
      <p className={styles.placeholder}>
        Volunteer events and RSVPs are coming in Phase 3. Check back soon!
      </p>
    </div>
  );
}
