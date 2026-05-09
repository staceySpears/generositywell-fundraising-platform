import client from './client.js';

/** @returns {Promise<import('../types').FundraisingEvent[]>} */
export const getAllEvents = async () => {
  const { data } = await client.get('/events/all');
  return data;
};

/** @param {string} id */
export const getEventById = async (id) => {
  const { data } = await client.get(`/events/${id}`);
  return data;
};

/** @param {string} campaignId */
export const getEventsByCampaign = async (campaignId) => {
  const { data } = await client.get(`/events/campaign/${campaignId}`);
  return data;
};

/**
 * @param {object} payload - CreateEventRequest shape
 * @returns {Promise<import('../types').FundraisingEvent>}
 */
export const createEvent = async (payload) => {
  const { data } = await client.post('/events', payload);
  return data;
};

/**
 * @param {string} eventId
 * @param {object} payload - EventUpdateRequest shape
 */
export const updateEvent = async (eventId, payload) => {
  const { data } = await client.put(`/events/${eventId}`, payload);
  return data;
};

/** @param {string} eventId */
export const publishEvent = async (eventId) => {
  const { data } = await client.post(`/events/${eventId}/publish`);
  return data;
};

/** @param {string} eventId */
export const cancelEvent = async (eventId) => {
  const { data } = await client.post(`/events/${eventId}/cancel`);
  return data;
};

/** @param {string} eventId */
export const deleteEvent = async (eventId) => {
  await client.delete(`/events/${eventId}`);
};

/**
 * Submit a volunteer RSVP.
 * @param {string} eventId
 * @param {{ volunteerId: string, volunteerName: string, volunteerEmail: string }} payload
 */
export const rsvpToEvent = async (eventId, payload) => {
  const { data } = await client.post(`/events/${eventId}/rsvp`, payload);
  return data;
};

/**
 * Cancel a volunteer's own RSVP.
 * @param {string} eventId
 * @param {string} volunteerId
 */
export const cancelRsvp = async (eventId, volunteerId) => {
  const { data } = await client.delete(`/events/${eventId}/rsvp/${volunteerId}`);
  return data;
};
