import { Link } from 'react-router-dom';
import styles from './ForgotPasswordPage.module.css';

/**
 * Placeholder — password reset requires a backend email flow (Phase 5).
 * UI-only for now; actual reset submission will be added once the endpoint exists.
 */
export default function ForgotPasswordPage() {
  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Reset your password</h1>
        <p className={styles.subtitle}>
          Password reset is coming soon. For now, contact your organizer or create a new account.
        </p>
        <Link to="/login" className={styles.backLink}>
          ← Back to login
        </Link>
      </div>
    </div>
  );
}
