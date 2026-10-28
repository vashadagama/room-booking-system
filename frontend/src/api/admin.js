import client from './client'

export const getAdminRooms = () => client.get('/admin/rooms')
export const getAdminSummary = () => client.get('/admin/summary')

export { createRoom, updateRoom, deleteRoom } from './rooms'