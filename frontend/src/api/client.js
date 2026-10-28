import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function errorText(e, fallback = 'Something went wrong') {
  const data = e.response?.data

  if (data?.errors && typeof data.errors === 'object') {
    return Object.values(data.errors).join('. ')
  }
  return data?.message || e.message || fallback
}

export default client