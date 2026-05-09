import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login as apiLogin, register as apiRegister } from '../api/userApi.js';

// Exported so useAuth (src/hooks/useAuth.js) can import it without creating
// a circular dependency with AuthProvider.
// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext(null);

/**
 * Provides { token, userId, isAuthenticated, login, register, logout } to the tree.
 *
 * Security note: the JWT is currently stored in localStorage for simplicity.
 * This is acceptable for this development milestone but should be migrated to
 * an HttpOnly, Secure, SameSite=Strict cookie (issued by Spring Security) before
 * production. See SECURITY.md for the trade-off discussion.
 *
 * Token + userId survive page refresh via localStorage, but AuthProvider listens
 * for two events so multi-tab and interceptor-triggered logouts propagate correctly:
 *  - 'storage': fires in other tabs when localStorage changes (cross-tab sync)
 *  - 'gw:auth:expired': fired by the Axios 401 interceptor (same-tab expiry)
 */
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('gw_token'));
  const [userId, setUserId] = useState(() => localStorage.getItem('gw_userId'));
  const navigate = useNavigate();

  // ── Cross-tab sync + interceptor-triggered logout ──────────────────────────
  useEffect(() => {
    const syncFromStorage = () => {
      setToken(localStorage.getItem('gw_token'));
      setUserId(localStorage.getItem('gw_userId'));
    };

    const handleExpired = () => {
      setToken(null);
      setUserId(null);
      navigate('/login', { replace: true });
    };

    // 'storage' only fires in *other* tabs — keeps them in sync on logout/login.
    window.addEventListener('storage', syncFromStorage);
    // 'gw:auth:expired' fires in the *same* tab when the 401 interceptor runs.
    window.addEventListener('gw:auth:expired', handleExpired);

    return () => {
      window.removeEventListener('storage', syncFromStorage);
      window.removeEventListener('gw:auth:expired', handleExpired);
    };
  }, [navigate]);

  // ── Auth actions ───────────────────────────────────────────────────────────
  const login = useCallback(async (credentials) => {
    const data = await apiLogin(credentials);
    localStorage.setItem('gw_token', data.token);
    localStorage.setItem('gw_userId', data.userId);
    setToken(data.token);
    setUserId(data.userId);
    return data;
  }, []);

  const register = useCallback(async (payload) => {
    // Registration does not return a token — caller redirects to /login.
    return apiRegister(payload);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('gw_token');
    localStorage.removeItem('gw_userId');
    setToken(null);
    setUserId(null);
  }, []);

  const value = useMemo(
    () => ({ token, userId, isAuthenticated: !!token, login, register, logout }),
    [token, userId, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
