import client from './client'

export const getBookingsForRoom = (roomId, date) =>
  client.get(`/bookings/room/${roomId}`, { params: { date } })

export const getMyBookings = (userId) =>
  client.get('/bookings/my', { params: { userId } })

export const createBooking = (data) => client.post('/bookings', data)

export const cancelBooking = (id, userId) =>
  client.delete(`/bookings/${id}`, { params: { userId } })