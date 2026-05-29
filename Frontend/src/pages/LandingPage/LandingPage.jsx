import { Link } from 'react-router-dom';
import styles from './LandingPage.module.css';

export default function LandingPage() {
  return (
    <div className={styles.hero}>
      <div className={styles.heroContent}>
        <h1 className={styles.title}>Fundraising rooted in community.</h1>
        <p className={styles.subtitle}>
          GenerosityWell is a hyperlocal platform for neighborhood groups, schools, and grassroots
          nonprofits. Track volunteer hours alongside donations, and close the loop with structured
          impact reporting — so every contributor sees exactly how their time or money drove
          real-world change.
        </p>
        <div className={styles.actions}>
          <Link to="/campaigns" className={styles.primaryBtn}>
            Browse campaigns
          </Link>
          <Link to="/create-campaign" className={styles.secondaryBtn}>
            Start a campaign
          </Link>
        </div>
      </div>

      <section className={styles.features}>
        <div className={styles.featureCard}>
          <span className={styles.featureIcon}>💰</span>
          <h3>Donation tracking</h3>
          <p>Every dollar in, tracked to the cent. Real-time progress bars keep donors engaged.</p>
        </div>
        <div className={styles.featureCard}>
          <span className={styles.featureIcon}>🤝</span>
          <h3>Volunteer coordination</h3>
          <p>
            Sweat equity counts too. Attendees RSVP for events and log volunteer hours alongside
            cash contributions.
          </p>
        </div>
        <div className={styles.featureCard}>
          <span className={styles.featureIcon}>📊</span>
          <h3>Giving history</h3>
          <p>
            Track your contributions over time. Your dashboard shows exactly how much you&apos;ve
            given to each campaign.
          </p>
        </div>
      </section>
    </div>
  );
}
