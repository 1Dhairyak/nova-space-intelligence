import { useEffect, useRef } from 'react'
import { useNovaStore } from '../store/novaStore'
import { spaceApi } from '../services/api'
import { connectWebSocket, disconnectWebSocket } from '../services/websocket'

/**
 * useSpaceData — called once from App.jsx
 *
 * On mount:
 *   1. Fires REST calls to populate store from Redis cache immediately
 *      (so the UI shows data before the first WebSocket push arrives)
 *   2. Opens the SockJS/STOMP WebSocket connection
 *   3. All subsequent updates arrive via WebSocket subscriptions in websocket.js
 *
 * On unmount: disconnects WebSocket cleanly.
 */
export function useSpaceData() {
  const { setIss, setLaunches, setNeos, setWeather, setApod } = useNovaStore()
  const connected = useRef(false)

  useEffect(() => {
    // ── 1. Seed store from REST (cache-hits are instant) ─────────────────
    const loadInitial = async () => {
      try {
        const [brief, iss, launches, neos, weather, apod] = await Promise.allSettled([
          spaceApi.getBrief(),
          spaceApi.getIss(),
          spaceApi.getLaunches(),
          spaceApi.getNeo(),
          spaceApi.getWeather(),
          spaceApi.getApod(),
        ])

        // Brief supersedes individual endpoints if available
        if (brief.status === 'fulfilled' && brief.value.data?.issPosition) {
          const b = brief.value.data
          if (b.issPosition)      setIss(b.issPosition)
          if (b.upcomingLaunches) setLaunches(b.upcomingLaunches)
          if (b.nearEarthObjects) setNeos(b.nearEarthObjects)
          if (b.spaceWeather)     setWeather(b.spaceWeather)
          if (b.apod)             setApod(b.apod)
          return
        }

        // Fallback to individual endpoints
        if (iss.status === 'fulfilled')      setIss(iss.value.data)
        if (launches.status === 'fulfilled') setLaunches(launches.value.data)
        if (neos.status === 'fulfilled')     setNeos(neos.value.data)
        if (weather.status === 'fulfilled')  setWeather(weather.value.data)
        if (apod.status === 'fulfilled')     setApod(apod.value.data)
      } catch (err) {
        console.error('[Nova] Initial data load failed:', err)
      }
    }

    loadInitial()

    // ── 2. Open WebSocket ────────────────────────────────────────────────
    if (!connected.current) {
      connectWebSocket()
      connected.current = true
    }

    return () => {
      disconnectWebSocket()
      connected.current = false
    }
  }, [])
}
