import { create } from 'zustand'

/**
 * Central Zustand store.
 * All WebSocket pushes and REST responses write here.
 * React components subscribe to slices they care about —
 * only those components re-render on change.
 */
export const useNovaStore = create((set, get) => ({

  // ─── Auth ────────────────────────────────────────────────────────────────
  token: localStorage.getItem('nova_token') || null,
  refreshToken: localStorage.getItem('nova_refresh') || null,
  user: null,

  setAuth: (token, refreshToken, user) => {
    localStorage.setItem('nova_token', token)
    localStorage.setItem('nova_refresh', refreshToken)
    set({ token, refreshToken, user })
  },

  logout: () => {
    localStorage.removeItem('nova_token')
    localStorage.removeItem('nova_refresh')
    set({ token: null, refreshToken: null, user: null })
  },

  // ─── WebSocket connection state ──────────────────────────────────────────
  wsConnected: false,
  setWsConnected: (v) => set({ wsConnected: v }),

  // ─── ISS ─────────────────────────────────────────────────────────────────
  iss: null,                     // { latitude, longitude, altitudeKm, speedKms, timestamp }
  issTrail: [],                  // last 60 positions for the orbit trail
  setIss: (pos) => set((state) => ({
    iss: pos,
    issTrail: [...state.issTrail.slice(-59), pos],
  })),

  // ─── Launches ────────────────────────────────────────────────────────────
  launches: [],                  // Launch[]
  setLaunches: (launches) => set({ launches }),

  // ─── NEO ─────────────────────────────────────────────────────────────────
  neos: [],                      // NearEarthObject[]
  setNeos: (neos) => set({ neos }),

  // ─── Space Weather ───────────────────────────────────────────────────────
  weather: null,                 // { kpIndex, stormLevel, flares[] }
  setWeather: (weather) => set({ weather }),

  // ─── APOD ────────────────────────────────────────────────────────────────
  apod: null,
  setApod: (apod) => set({ apod }),

  // ─── Nova Chat ───────────────────────────────────────────────────────────
  chatMessages: [
    {
      id: '0',
      role: 'nova',
      text: 'Nova online. Connecting to live telemetry feeds…',
      ts: new Date().toISOString(),
    },
  ],
  isChatLoading: false,

  addChatMessage: (msg) => set((state) => ({
    chatMessages: [...state.chatMessages, { id: Date.now().toString(), ...msg }],
  })),

  setChatLoading: (v) => set({ isChatLoading: v }),

  // ─── Active nav section ──────────────────────────────────────────────────
  activeSection: 'dashboard',
  setActiveSection: (s) => set({ activeSection: s }),
}))
