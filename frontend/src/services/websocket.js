import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useNovaStore } from '../store/novaStore'

/**
 * Singleton WebSocket service.
 *
 * Topics subscribed:
 *   /topic/iss        → ISS position (every 5s)
 *   /topic/launches   → launch list (every 5min)
 *   /topic/neo        → asteroid feed (every 1hr)
 *   /topic/weather    → space weather (every 5min)
 *   /topic/brief      → full brief (every 5min)
 *   /user/queue/nova  → Nova AI chat responses (personal queue)
 *
 * Sends to:
 *   /app/nova/chat    → user chat messages to Nova
 */

let stompClient = null

export function connectWebSocket() {
  const token = useNovaStore.getState().token

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),

    connectHeaders: {
      Authorization: token ? `Bearer ${token}` : '',
    },

    // Reconnect after 5 seconds on disconnect
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('[Nova WS] Connected')
      useNovaStore.getState().setWsConnected(true)

      // ─── Subscribe to broadcast topics ─────────────────────────────────

      stompClient.subscribe('/topic/iss', (msg) => {
        const pos = JSON.parse(msg.body)
        useNovaStore.getState().setIss(pos)
      })

      stompClient.subscribe('/topic/launches', (msg) => {
        const launches = JSON.parse(msg.body)
        useNovaStore.getState().setLaunches(launches)
      })

      stompClient.subscribe('/topic/neo', (msg) => {
        const neos = JSON.parse(msg.body)
        useNovaStore.getState().setNeos(neos)
      })

      stompClient.subscribe('/topic/weather', (msg) => {
        const weather = JSON.parse(msg.body)
        useNovaStore.getState().setWeather(weather)
      })

      stompClient.subscribe('/topic/brief', (msg) => {
        const brief = JSON.parse(msg.body)
        const store = useNovaStore.getState()
        if (brief.issPosition)       store.setIss(brief.issPosition)
        if (brief.upcomingLaunches)  store.setLaunches(brief.upcomingLaunches)
        if (brief.nearEarthObjects)  store.setNeos(brief.nearEarthObjects)
        if (brief.spaceWeather)      store.setWeather(brief.spaceWeather)
        if (brief.apod)              store.setApod(brief.apod)
      })

      // ─── Subscribe to personal Nova chat queue ──────────────────────────
      stompClient.subscribe('/user/queue/nova', (msg) => {
        const payload = JSON.parse(msg.body)
        useNovaStore.getState().addChatMessage({
          role: 'nova',
          text: payload.text,
          ts: new Date().toISOString(),
        })
        useNovaStore.getState().setChatLoading(false)
      })
    },

    onDisconnect: () => {
      console.log('[Nova WS] Disconnected')
      useNovaStore.getState().setWsConnected(false)
    },

    onStompError: (frame) => {
      console.error('[Nova WS] STOMP error', frame.headers['message'])
    },
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  if (stompClient?.active) {
    stompClient.deactivate()
  }
}

/**
 * Send a chat message to Nova via WebSocket.
 * The response arrives asynchronously on /user/queue/nova.
 */
export function sendNovaMessage(message, conversationId) {
  if (!stompClient?.active) {
    console.warn('[Nova WS] Not connected — cannot send message')
    return false
  }
  stompClient.publish({
    destination: '/app/nova/chat',
    body: JSON.stringify({ message, conversationId }),
  })
  return true
}
