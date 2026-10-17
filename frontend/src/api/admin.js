import client from './client'

/** Данные админ-панели. Доступны только с ролью ADMIN. */
export const getAdminRooms = () => client.get('/admin/rooms')
export const getAdminSummary = () => client.get('/admin/summary')

// Создание, правка и отключение комнат живут на /rooms — там уже есть
// правило hasRole('ADMIN'), свои дубли не заводим.
export { createRoom, updateRoom, deleteRoom } from './rooms'