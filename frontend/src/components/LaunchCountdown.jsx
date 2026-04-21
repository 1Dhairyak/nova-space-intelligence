import { useNovaStore } from '../store/novaStore'
import { useState, useEffect } from 'react'
import { formatDistanceToNow, isPast } from 'date-fns'

/**
 * Shows next launch countdown ticking in real time.
 * Data from WebSocket → Zustand store.
 */
export default function LaunchCountdown() {
  const launches = useNovaStore((s) => s.launches)
  const [tick, setTick] = useState(0)

  // Re-render every second for the countdown
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 1000)
    return () => clearInterval(id)
  }, [])

  // Find next upcoming launch with a confirmed window
  const next = launches
    .filter((l) => l.windowStart && !isPast(new Date(l.windowStart)))
    .sort((a, b) => new Date(a.windowStart) - new Date(b.windowStart))[0]

  if (!next) {
    return (
      <div style={{ color: 'rgba(120,165,200,0.6)', fontSize: 12 }}>
        No confirmed launches with window start times available.
      </div>
    )
  }

  const diff = Math.max(0, Math.floor((new Date(next.windowStart) - Date.now()) / 1000))
  const h = Math.floor(diff / 3600)
  const m = Math.floor((diff % 3600) / 60)
  const s = diff % 60
  const pad = (n) => String(n).padStart(2, '0')

  // Progress bar — assume 8-hour window
  const totalWindow = 8 * 3600
  const elapsed = totalWindow - diff
  const pct = Math.min(100, Math.round((elapsed / totalWindow) * 100))

  const agencyColor = {
    SPACEX: '#00e5ff',
    NASA:   '#4da6ff',
    ISRO:   '#ffa500',
  }[next.agencyCode] || '#aaa'

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
        <div>
          <div style={{ fontSize: 15, fontWeight: 600, color: '#c8e0f4' }}>{next.name}</div>
          <div style={{ fontSize: 10, color: agencyColor, letterSpacing: 2, textTransform: 'uppercase', marginBottom: 10 }}>
            {next.agency} · {next.launchSite || 'TBD'}
          </div>
        </div>
        <span style={{
          fontSize: 9, padding: '2px 6px', borderRadius: 2,
          background: 'rgba(0,255,157,0.15)', color: '#00ff9d',
          border: '1px solid rgba(0,255,157,0.3)',
          fontFamily: '"Space Mono", monospace', height: 'fit-content'
        }}>T- COUNTING</span>
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
        {[['HRS', pad(h)], ['MIN', pad(m)], ['SEC', pad(s)]].map(([label, val], i) => (
          <span key={label} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ textAlign: 'center' }}>
              <div style={{
                fontFamily: '"Space Mono", monospace', fontSize: 26, fontWeight: 700,
                color: '#00e5ff', textShadow: '0 0 20px rgba(0,229,255,0.4)'
              }}>{val}</div>
              <div style={{ fontSize: 9, color: 'rgba(120,165,200,0.6)', letterSpacing: 2 }}>{label}</div>
            </span>
            {i < 2 && <span style={{ fontFamily: '"Space Mono", monospace', fontSize: 22, color: '#3d6280', marginTop: -6 }}>:</span>}
          </span>
        ))}
      </div>

      <div style={{ fontSize: 10, color: '#7aa5c8', marginBottom: 8 }}>
        Launch site: <span style={{ color: agencyColor }}>{next.launchSite}</span>
        {next.missionDesc && next.missionDesc !== 'TBD' && (
          <> · <span style={{ color: '#7aa5c8' }}>{next.missionDesc.slice(0, 60)}{next.missionDesc.length > 60 ? '…' : ''}</span></>
        )}
      </div>

      <div style={{ height: 2, background: 'rgba(26,48,80,1)', borderRadius: 1, overflow: 'hidden' }}>
        <div style={{
          height: '100%', width: pct + '%',
          background: 'linear-gradient(90deg, #00e5ff, #00ff9d)',
          transition: 'width 1s linear'
        }} />
      </div>
    </div>
  )
}
