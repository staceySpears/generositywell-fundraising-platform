import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';

/**
 * Returns the current auth context.
 * Must be called inside an AuthProvider.
 *
 * @returns {{ token: string|null, userId: string|null, isAuthenticated: boolean, login: Function, register: Function, logout: Function }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
