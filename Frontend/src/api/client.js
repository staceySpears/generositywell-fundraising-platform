import axios from 'axios';

/**
 * Central Axios instance.
 *
 * In development, Vite proxies /api/* → http://localhost:5001/*.
 * In production set VITE_API_BASE_URL to your API domain.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
});

// Attach the JWT on every request if one is stored.
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('gw_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, clear stored credentials and dispatch an event so AuthProvider
// can call logout() and React Router can redirect to /login.
// We can't import AuthContext here (circular dep), so we use a custom event.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('gw_token');
      localStorage.removeItem('gw_userId');
      window.dispatchEvent(new CustomEvent('gw:auth:expired'));
    }
    return Promise.reject(error);
  }
);

export default client;
