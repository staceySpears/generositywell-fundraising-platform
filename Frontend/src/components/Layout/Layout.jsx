import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import styles from './Layout.module.css';

export default function Layout() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className={styles.root}>
      <nav className={styles.nav}>
        <Link to="/" className={styles.brand}>GenerosityWell</Link>

        <ul className={styles.navLinks}>
          <li><NavLink to="/campaigns" className={({ isActive }) => isActive ? styles.active : ''}>Campaigns</NavLink></li>
          <li><NavLink to="/search"    className={({ isActive }) => isActive ? styles.active : ''}>Search</NavLink></li>

          {isAuthenticated ? (
            <>
              <li><NavLink to="/dashboard" className={({ isActive }) => isActive ? styles.active : ''}>Dashboard</NavLink></li>
              <li><NavLink to="/calendar"  className={({ isActive }) => isActive ? styles.active : ''}>Calendar</NavLink></li>
              <li><NavLink to="/events"    className={({ isActive }) => isActive ? styles.active : ''}>Events</NavLink></li>
              <li>
                <button className={styles.logoutBtn} onClick={handleLogout}>
                  Log out
                </button>
              </li>
            </>
          ) : (
            <>
              <li><NavLink to="/login"    className={({ isActive }) => isActive ? styles.active : ''}>Log in</NavLink></li>
              <li><NavLink to="/register" className={`${styles.registerBtn} ${styles.active}`}>Join</NavLink></li>
            </>
          )}
        </ul>
      </nav>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
