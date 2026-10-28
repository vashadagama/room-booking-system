import client from './client'

export const getBookingsForRoom = (roomId, date) =>
  client.get(`/bookings/room/${roomId}`, { params: { date } })

export const getMyBookings = () => client.get('/bookings/my')

export const createBooking = (data) => client.post('/bookings', data)

export const cancelBooking = (id) => client.delete(`/bookings/${id}`)