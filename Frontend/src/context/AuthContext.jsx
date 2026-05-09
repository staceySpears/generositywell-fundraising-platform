import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/userApi.js';

const AuthContext = createContext(null);

/**
 * Provides { user, token, login, register, logout } to the component tree.
 * Token + userId survive page refresh via localStorage.
 */
export function AuthProvider({ children }) {
  const [token, setToken]   = useState(() => localStorage.getItem('gw_token'));
  const [userId, setUserId] = useState(() => localStorage.getItem('gw_userId'));

  const login = useCallback(async (credentials) => {
    const data = await apiLogin(credentials);
    localStorage.setItem('gw_token',  data.token);
    localStorage.setItem('gw_userId', data.userId);
    setToken(data.token);
    setUserId(data.userId);
    return data;
  }, []);

  const register = useCallback(async (payload) => {
    const data = await apiRegister(payload);
    // Registration does not return a token — redirect to /login after.
    return data;
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

/** @returns {{ token: string|null, userId: string|null, isAuthenticated: boolean, login: Function, register: Function, logout: Function }} */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
