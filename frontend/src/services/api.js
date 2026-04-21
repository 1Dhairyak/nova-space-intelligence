import axios from 'axios'
import { useNovaStore } from '../store/novaStore'

/**
 * Axios instance with JWT interceptor.
 * - Attaches Authorization header from store on every request
 * - Handles 401 → attempts token refresh → retries original request
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// Request interceptor — attach JWT
api.interceptors.request.use((config) => {
  const token = useNovaStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor — handle 401 refresh
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = useNovaStore.getState().refreshToken
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const { token, refreshToken: newRefresh, user } = res.data
        useNovaStore.getState().setAuth(token, newRefresh, user)
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      } catch {
        useNovaStore.getState().logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// ─── Auth ─────────────────────────────────────────────────────────────────

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
  me:       ()     => api.get('/auth/me'),
}

// ─── Space Data (REST — used for initial page load) ────────────────────────

export const spaceApi = {
  getBrief:          () => api.get('/space/brief'),
  getIss:            () => api.get('/space/iss'),
  getLaunches:       () => api.get('/space/launches'),
  getIsroLaunches:   () => api.get('/space/launches/isro'),
  getSpaceXLaunches: () => api.get('/space/launches/spacex'),
  getNeo:            () => api.get('/space/neo'),
  getWeather:        () => api.get('/space/weather'),
  getApod:           () => api.get('/space/apod'),
}

export default api
