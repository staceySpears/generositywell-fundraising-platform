import client from './client.js';

/**
 * POST /auth/login
 * @param {{ email: string, password: string }} credentials
 * @returns {Promise<{ token: string, userId: string }>}
 */
export const login = async (credentials) => {
  const { data } = await client.post('/auth/login', credentials);
  return data;
};

/**
 * POST /users
 * @param {{ name: string, email: string, password: string }} payload
 * @returns {Promise<import('../types').User>}
 */
export const register = async (payload) => {
  const { data } = await client.post('/users', payload);
  return data;
};

/**
 * GET /users/:id
 * @param {string} id
 */
export const getUserById = async (id) => {
  const { data } = await client.get(`/users/${id}`);
  return data;
};

/**
 * PUT /users/:id
 * @param {object} payload - UserUpdateRequest shape
 */
export const updateUser = async (payload) => {
  const { data } = await client.put(`/users/${payload.id}`, payload);
  return data;
};
