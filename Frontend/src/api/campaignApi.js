import client from './client.js';

/** @returns {Promise<import('../types').Campaign[]>} */
export const getAllCampaigns = async () => {
  const { data } = await client.get('/campaigns/all');
  return data;
};

/** @param {string} id */
export const getCampaignById = async (id) => {
  const { data } = await client.get(`/campaigns/${id}`);
  return data;
};

/**
 * @param {object} payload - CreateCampaignRequest shape
 * @returns {Promise<import('../types').Campaign>}
 */
export const createCampaign = async (payload) => {
  const { data } = await client.post('/campaigns', payload);
  return data;
};

/**
 * @param {string} campaignId
 * @param {object} payload - CampaignUpdateRequest shape
 */
export const updateCampaign = async (campaignId, payload) => {
  const { data } = await client.put(`/campaigns/${campaignId}`, payload);
  return data;
};

/**
 * Creates a Stripe PaymentIntent for the given campaign and amount.
 * Returns { clientSecret, paymentIntentId, amount }.
 * Requires a valid JWT (Authorization header set by the Axios interceptor).
 *
 * @param {string} campaignId
 * @param {number} amountInCents
 * @returns {Promise<{ clientSecret: string, paymentIntentId: string, amount: number }>}
 */
export const createPaymentIntent = async (campaignId, amountInCents) => {
  const { data } = await client.post(`/campaigns/${campaignId}/payment-intent`, {
    amount: amountInCents,
  });
  return data;
};

/**
 * @param {string} campaignId
 * @param {number} amountInCents
 */
export const donate = async (campaignId, amountInCents) => {
  const { data } = await client.post(`/campaigns/${campaignId}/donate`, {
    amount: amountInCents,
  });
  return data;
};

/** @param {string} campaignId */
export const closeCampaign = async (campaignId) => {
  const { data } = await client.post(`/campaigns/${campaignId}/close`);
  return data;
};

/** @param {string} campaignId */
export const deleteCampaign = async (campaignId) => {
  await client.delete(`/campaigns/${campaignId}`);
};
