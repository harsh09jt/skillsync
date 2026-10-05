import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('skillsync.token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function apiErrorMessage(error) {
  return error.response?.data?.message
    || error.response?.data?.detail
    || error.response?.data?.error
    || error.response?.statusText
    || (error.code === 'ERR_NETWORK'
      ? 'Cannot reach the SkillSync API. Make sure the backend is running.'
      : error.message)
}
