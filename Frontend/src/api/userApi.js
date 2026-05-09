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

/**
 * GET /users/:id/campaigns
 * Returns all campaigns created by the user.
 * @param {string} id
 * @returns {Promise<Array>}
 */
export const getUserCampaigns = async (id) => {
  const { data } = await client.get(`/users/${id}/campaigns`);
  return data;
};

/**
 * GET /users/:id/events
 * Returns all events organized by the user.
 * @param {string} id
 * @returns {Promise<Array>}
 */
export const getUserEvents = async (id) => {
  const { data } = await client.get(`/users/${id}/events`);
  return data;
};

/**
 * GET /users/:id/donations
 * Returns the user's giving history (one entry per campaign donated to).
 * @param {string} id
 * @returns {Promise<Array<{campaignId, campaignName, amountInCents, donationDate}>>}
 */
export const getUserDonations = async (id) => {
  const { data } = await client.get(`/users/${id}/donations`);
  return data;
};

/**
 * GET /users/:id/rsvps
 * Returns all events the user has an active RSVP for.
 * @param {string} id
 * @returns {Promise<Array>}
 */
export const getUserRsvps = async (id) => {
  const { data } = await client.get(`/users/${id}/rsvps`);
  return data;
};
