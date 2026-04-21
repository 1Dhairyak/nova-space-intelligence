import { useNovaStore } from '../store/novaStore'
import { useEffect, useRef } from 'react'

/**
 * ISSMap renders a canvas-based world map with:
 *  - A grid representing lat/lon
 *  - The ISS dot animating to each new position
 *  - A trailing orbital path (last 60 points)
 *  - Coordinates display
 *
 * Data comes entirely from the Zustand store (set by WebSocket).
 */
export default function ISSMap() {
  const iss      = useNovaStore((s) => s.iss)
  const trail    = useNovaStore((s) => s.issTrail)
  const canvasRef = useRef(null)

  const latToY = (lat, h) => ((90 - lat) / 180) * h
  const lonToX = (lon, w) => ((lon + 180) / 360) * w

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !iss) return
    const ctx  = canvas.getContext('2d')
    const W    = canvas.width
    const H    = canvas.height

    // Clear
    ctx.clearRect(0, 0, W, H)
    ctx.fillStyle = 'rgba(5, 10, 20, 0.8)'
    ctx.fillRect(0, 0, W, H)

    // Grid
    ctx.strokeStyle = 'rgba(0, 229, 255, 0.06)'
    ctx.lineWidth = 0.5
    for (let lat = -60; lat <= 60; lat += 30) {
      const y = latToY(lat, H)
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke()
    }
    for (let lon = -180; lon <= 180; lon += 60) {
      const x = lonToX(lon, W)
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke()
    }

    // Equator highlight
    ctx.strokeStyle = 'rgba(0, 229, 255, 0.15)'
    const eqY = latToY(0, H)
    ctx.beginPath(); ctx.moveTo(0, eqY); ctx.lineTo(W, eqY); ctx.stroke()

    // Orbital trail
    if (trail.length > 1) {
      ctx.beginPath()
      ctx.strokeStyle = 'rgba(0, 255, 157, 0.3)'
      ctx.lineWidth = 1.5
      ctx.setLineDash([4, 3])
      trail.forEach((p, i) => {
        const x = lonToX(p.longitude, W)
        const y = latToY(p.latitude, H)
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      })
      ctx.stroke()
      ctx.setLineDash([])
    }

    // ISS dot
    const x = lonToX(iss.longitude, W)
    const y = latToY(iss.latitude, H)

    // Glow
    const grad = ctx.createRadialGradient(x, y, 0, x, y, 14)
    grad.addColorStop(0, 'rgba(0, 255, 157, 0.6)')
    grad.addColorStop(1, 'rgba(0, 255, 157, 0)')
    ctx.fillStyle = grad
    ctx.beginPath(); ctx.arc(x, y, 14, 0, Math.PI * 2); ctx.fill()

    // Core dot
    ctx.fillStyle = '#00ff9d'
    ctx.beginPath(); ctx.arc(x, y, 4, 0, Math.PI * 2); ctx.fill()

    // Label
    ctx.fillStyle = '#00ff9d'
    ctx.font = '9px "Space Mono", monospace'
    ctx.fillText('ISS', x + 7, y - 7)
  }, [iss, trail])

  return (
    <div style={{ position: 'relative' }}>
      <canvas
        ref={canvasRef}
        width={560}
        height={140}
        style={{ width: '100%', height: 140, borderRadius: 4, display: 'block' }}
      />
      {iss && (
        <div style={{
          position: 'absolute', bottom: 6, left: 8,
          fontFamily: '"Space Mono", monospace', fontSize: 9,
          color: 'rgba(120, 165, 200, 0.8)'
        }}>
          LAT: {iss.latitude?.toFixed(2)}° | LON: {iss.longitude?.toFixed(2)}° | ALT: {iss.altitudeKm} km
        </div>
      )}
      <div style={{
        position: 'absolute', top: 6, right: 8,
        fontFamily: '"Space Mono", monospace', fontSize: 9,
        color: 'rgba(0, 229, 255, 0.4)'
      }}>
        OPEN NOTIFY API
      </div>
    </div>
  )
}
