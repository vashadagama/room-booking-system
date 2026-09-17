import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

// В лабе 7 заменить на данные из токена.
export const MOCK_USER_ID = 1
export const MOCK_USER_NAME = 'Test User'

export default client