import { useEffect, useState } from 'react'
import { useNovaStore } from './store/novaStore'
import { useSpaceData } from './hooks/useSpaceData'
import ISSMap from './components/ISSMap'
import LaunchCountdown from './components/LaunchCountdown'
import NovaChat from './components/NovaChat'

// ─── Utility ──────────────────────────────────────────────────────────────────

function pad(n) { return String(n).padStart(2, '0') }

function utcClock() {
  const now = new Date()
  return `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}:${pad(now.getUTCSeconds())} UTC`
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function PlanetAvatar({ size = 'sm' }) {
  const isLg = size === 'lg'
  const outer = isLg ? 48 : 38
  const planet = isLg ? 36 : 30
  const moon = isLg ? 11 : 9
  const orbitR = isLg ? 24 : 19
  return (
    <div style={{ position: 'relative', width: outer, height: outer, flexShrink: 0 }}>
      <div className="glow-ring" style={{ width: outer, height: outer }} />
      <div className="planet" style={{ width: planet, height: planet, top: (outer - planet) / 2, left: (outer - planet) / 2 }} />
      <div className="moon" style={{
        width: moon, height: moon,
        top: 0, left: '50%',
        '--orbit-r': orbitR + 'px',
        animation: `moonOrbit 3s linear infinite`
      }} />
    </div>
  )
}

function Card({ title, badge, badgeType = 'live', children, style = {} }) {
  const badgeColors = {
    live:  { bg: 'rgba(0,255,157,0.15)', color: '#00ff9d', border: 'rgba(0,255,157,0.3)' },
    warn:  { bg: 'rgba(255,165,0,0.15)', color: '#ffa500', border: 'rgba(255,165,0,0.3)' },
    alert: { bg: 'rgba(255,68,68,0.15)', color: '#ff4444', border: 'rgba(255,68,68,0.3)' },
    multi: { bg: 'rgba(0,229,255,0.1)',  color: '#00e5ff', border: 'rgba(0,229,255,0.2)' },
  }
  const bc = badgeColors[badgeType] || badgeColors.live
  return (
    <div style={{ background: 'rgba(10,18,32,0.85)', border: '1px solid rgba(26,48,80,1)', borderRadius: 6, padding: 12, ...style }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
        <span style={{ fontFamily: '"Space Mono", monospace', fontSize: 10, color: '#00e5ff', letterSpacing: 3, textTransform: 'uppercase' }}>{title}</span>
        {badge && (
          <span style={{ fontSize: 9, padding: '2px 6px', borderRadius: 2, fontFamily: '"Space Mono", monospace', background: bc.bg, color: bc.color, border: `1px solid ${bc.border}` }}>{badge}</span>
        )}
      </div>
      {children}
    </div>
  )
}

function StatCard({ value, label, sub, color = '#00e5ff' }) {
  return (
    <div style={{ background: 'rgba(13,24,40,0.9)', border: '1px solid rgba(26,48,80,1)', borderRadius: 4, padding: 10, textAlign: 'center' }}>
      <div style={{ fontFamily: '"Space Mono", monospace', fontSize: 18, fontWeight: 700, color }}>{value}</div>
      <div style={{ fontSize: 10, color: 'rgba(61,98,128,1)', letterSpacing: 1, marginTop: 2, textTransform: 'uppercase' }}>{label}</div>
      {sub && <div style={{ fontSize: 10, color: '#7aa5c8', marginTop: 4 }}>{sub}</div>}
    </div>
  )
}

function Pill({ children, type = 'green' }) {
  const colors = {
    green:  { bg: 'rgba(0,255,157,0.1)',  color: '#00ff9d', border: 'rgba(0,255,157,0.2)' },
    amber:  { bg: 'rgba(255,165,0,0.1)',  color: '#ffa500', border: 'rgba(255,165,0,0.2)' },
    red:    { bg: 'rgba(255,68,68,0.1)',  color: '#ff4444', border: 'rgba(255,68,68,0.3)' },
    cyan:   { bg: 'rgba(0,229,255,0.1)',  color: '#00e5ff', border: 'rgba(0,229,255,0.2)' },
  }
  const c = colors[type] || colors.green
  return (
    <span style={{ display: 'inline-block', padding: '1px 5px', borderRadius: 2, fontSize: 9, background: c.bg, color: c.color, border: `1px solid ${c.border}` }}>
      {children}
    </span>
  )
}

// ─── Main App ─────────────────────────────────────────────────────────────────

export default function App() {
  useSpaceData()

  const iss        = useNovaStore((s) => s.iss)
  const launches   = useNovaStore((s) => s.launches)
  const neos       = useNovaStore((s) => s.neos)
  const weather    = useNovaStore((s) => s.weather)
  const apod       = useNovaStore((s) => s.apod)
  const wsConnected = useNovaStore((s) => s.wsConnected)
  const activeSection = useNovaStore((s) => s.activeSection)
  const setSection = useNovaStore((s) => s.setActiveSection)

  const [clock, setClock] = useState(utcClock())

  useEffect(() => {
    const id = setInterval(() => setClock(utcClock()), 1000)
    return () => clearInterval(id)
  }, [])

  const activeFlares = weather?.flares?.filter(f => f.status === 'ACTIVE') ?? []
  const hazardousNeos = neos.filter(n => n.potentiallyHazardous)
  const nextLaunch = launches.find(l => l.windowStart && new Date(l.windowStart) > new Date())

  const navItems = [
    { id: 'dashboard', icon: '⬡', label: 'DASHBOARD' },
    { id: 'iss',       icon: '◎', label: 'ISS TRACKER',  badge: 'LIVE', badgeType: 'cyan' },
    { id: 'launches',  icon: '◈', label: 'LAUNCHES',     badge: launches.length || null, badgeType: 'amber' },
    { id: 'asteroids', icon: '◇', label: 'ASTEROIDS',    badge: neos.length || null },
  ]

  const agencyItems = [
    { id: 'nasa',   icon: '▲', label: 'NASA' },
    { id: 'isro',   icon: '▲', label: 'ISRO' },
    { id: 'spacex', icon: '▲', label: 'SPACEX' },
  ]

  return (
    <div style={{ background: '#050a14', color: '#c8e0f4', fontFamily: 'Rajdhani, sans-serif', fontSize: 14, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden', position: 'relative' }}>

      {/* Stars */}
      <Stars />

      {/* ── NAV ─────────────────────────────────────────────────────────── */}
      <nav style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 20px', background: 'rgba(5,10,20,0.95)', borderBottom: '1px solid rgba(26,48,80,1)', zIndex: 10, flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <PlanetAvatar size="sm" />
          <div>
            <div style={{ fontFamily: '"Space Mono", monospace', fontSize: 16, fontWeight: 700, color: '#00e5ff', letterSpacing: 4 }}>NOVA</div>
            <div style={{ fontSize: 10, color: 'rgba(61,98,128,1)', letterSpacing: 3, textTransform: 'uppercase' }}>Space Intelligence System</div>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontFamily: '"Space Mono", monospace', fontSize: 11, color: wsConnected ? '#00ff9d' : '#ffa500' }}>
          <div style={{ width: 7, height: 7, borderRadius: '50%', background: wsConnected ? '#00ff9d' : '#ffa500', animation: 'blink 2s ease-in-out infinite', boxShadow: wsConnected ? '0 0 10px rgba(0,255,157,0.5)' : '0 0 10px rgba(255,165,0,0.5)' }} />
          {wsConnected ? 'ALL SYSTEMS NOMINAL' : 'CONNECTING…'}
        </div>
        <div style={{ fontFamily: '"Space Mono", monospace', fontSize: 12, color: '#7aa5c8', letterSpacing: 1 }}>{clock}</div>
      </nav>

      {/* ── BODY ─────────────────────────────────────────────────────────── */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative', zIndex: 1 }}>

        {/* ── SIDEBAR ──────────────────────────────────────────────────── */}
        <div style={{ width: 200, background: 'rgba(10,18,32,0.9)', borderRight: '1px solid rgba(26,48,80,1)', display: 'flex', flexDirection: 'column', padding: '12px 0', flexShrink: 0 }}>
          <SideSection label="Navigation" />
          {navItems.map(item => (
            <NavItem key={item.id} {...item} active={activeSection === item.id} onClick={() => setSection(item.id)} />
          ))}
          <SideSection label="Agencies" />
          {agencyItems.map(item => (
            <NavItem key={item.id} {...item} active={activeSection === item.id} onClick={() => setSection(item.id)} />
          ))}
          <SideSection label="Alerts" />
          {activeFlares.length > 0 && (
            <div style={{ margin: '0 12px 8px', padding: '8px 10px', background: 'rgba(255,68,68,0.1)', border: '1px solid rgba(255,68,68,0.3)', borderRadius: 4, fontSize: 11, color: '#ff8888', lineHeight: 1.4 }}>
              <b style={{ display: 'block', fontFamily: '"Space Mono", monospace', fontSize: 10, color: '#ff4444', marginBottom: 3 }}>⚠ SOLAR FLARE</b>
              {activeFlares[0].classType} class active. Minor radio disruption possible.
            </div>
          )}
          <NavItem id="weather" icon="◉" label="SPACE WX" badge={activeFlares.length || null} active={activeSection === 'weather'} onClick={() => setSection('weather')} />
          <NavItem id="apod" icon="◎" label="APOD" active={activeSection === 'apod'} onClick={() => setSection('apod')} />
        </div>

        {/* ── CENTER ───────────────────────────────────────────────────── */}
        <div style={{ flex: 1, overflowY: 'auto', padding: 14, display: 'flex', flexDirection: 'column', gap: 12, scrollbarWidth: 'thin', scrollbarColor: 'rgba(26,48,80,1) transparent' }}>

          {/* Daily Brief */}
          <Card title="Daily Mission Brief" badge="LIVE BRIEF">
            <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
              <div style={{ width: 80, height: 60, borderRadius: 4, border: '1px solid rgba(26,48,80,1)', flexShrink: 0, background: 'linear-gradient(135deg,#1a0a3a,#0a1a3a)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24 }}>
                {apod?.mediaType === 'image' && apod?.url ? (
                  <img src={apod.url} alt={apod.title} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 4 }} />
                ) : '🌌'}
              </div>
              <div>
                <div style={{ fontSize: 12, fontWeight: 600, color: '#c8e0f4', marginBottom: 3 }}>
                  {apod?.title || 'Loading APOD…'}
                </div>
                <div style={{ fontSize: 11, color: '#7aa5c8', lineHeight: 1.4 }}>
                  {iss ? `ISS at ${iss.latitude?.toFixed(1)}°, ${iss.longitude?.toFixed(1)}° · ` : ''}
                  {nextLaunch ? `Next launch: ${nextLaunch.name} (${nextLaunch.agencyCode}) · ` : ''}
                  {neos.length > 0 ? `${neos.length} NEOs tracked · ` : ''}
                  {activeFlares.length > 0 ? `${activeFlares.length} active solar flare(s)` : 'Space weather nominal'}
                </div>
              </div>
            </div>
            <div style={{ marginTop: 8, display: 'flex', gap: 16 }}>
              <span style={{ fontSize: 10, color: 'rgba(61,98,128,1)', fontFamily: '"Space Mono", monospace' }}>DATE: <span style={{ color: '#00e5ff' }}>{new Date().toISOString().split('T')[0]}</span></span>
              <span style={{ fontSize: 10, color: 'rgba(61,98,128,1)', fontFamily: '"Space Mono", monospace' }}>STATUS: <span style={{ color: '#00ff9d' }}>NOMINAL</span></span>
              <span style={{ fontSize: 10, color: 'rgba(61,98,128,1)', fontFamily: '"Space Mono", monospace' }}>THREATS: <span style={{ color: hazardousNeos.length > 0 ? '#ff4444' : '#ffa500' }}>{hazardousNeos.length > 0 ? 'ELEVATED' : 'LOW'}</span></span>
            </div>
          </Card>

          {/* Stat grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 8 }}>
            <StatCard value={iss ? iss.speedKms?.toFixed(2) : '—'} label="ISS Speed km/s" sub={iss ? `${iss.altitudeKm} km alt` : 'Loading…'} color="#00ff9d" />
            <StatCard value={launches.length || '—'} label="Upcoming Launches" sub={nextLaunch ? nextLaunch.agencyCode : 'Loading…'} color="#ffa500" />
            <StatCard value={hazardousNeos.length || neos.length || '—'} label={hazardousNeos.length ? 'Hazardous NEOs' : 'NEOs Tracked'} sub="This week" color="#ff4444" />
            <StatCard value={weather?.kpIndex?.toFixed(1) ?? '—'} label="Kp Index" sub={weather?.stormLevel ? `Storm: ${weather.stormLevel}` : 'Loading…'} color="#00e5ff" />
          </div>

          {/* ISS Map */}
          <Card title="ISS Live Track" badge="UPDATING">
            <ISSMap />
          </Card>

          {/* NEO + Solar Flares */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <Card title="Near Earth Objects" badge={`${neos.length} THIS WEEK`} badgeType="warn" style={{ padding: 10 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    {['Name','Dist AU','Risk'].map(h => (
                      <th key={h} style={{ fontSize: 9, color: 'rgba(61,98,128,1)', letterSpacing: 2, textAlign: 'left', padding: '4px 6px', borderBottom: '1px solid rgba(26,48,80,1)', textTransform: 'uppercase' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {neos.slice(0, 5).map((neo, i) => (
                    <tr key={neo.id || i}>
                      <td style={{ fontSize: 12, color: '#c8e0f4', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>{neo.name?.replace('(', '').replace(')', '').trim()}</td>
                      <td style={{ fontSize: 11, color: '#7aa5c8', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)', fontFamily: '"Space Mono", monospace' }}>{neo.distanceAu?.toFixed(4)}</td>
                      <td style={{ padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>
                        <Pill type={neo.potentiallyHazardous ? 'red' : neo.distanceAu < 0.01 ? 'amber' : 'green'}>
                          {neo.potentiallyHazardous ? 'HAZARD' : neo.distanceAu < 0.01 ? 'WATCH' : 'SAFE'}
                        </Pill>
                      </td>
                    </tr>
                  ))}
                  {neos.length === 0 && (
                    <tr><td colSpan={3} style={{ padding: 12, textAlign: 'center', color: 'rgba(61,98,128,1)', fontSize: 11 }}>Loading NEO data…</td></tr>
                  )}
                </tbody>
              </table>
            </Card>

            <Card title="Solar Activity" badge={activeFlares.length > 0 ? 'FLARE ACTIVE' : 'NOMINAL'} badgeType={activeFlares.length > 0 ? 'alert' : 'live'} style={{ padding: 10 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    {['Class','Start UTC','Status'].map(h => (
                      <th key={h} style={{ fontSize: 9, color: 'rgba(61,98,128,1)', letterSpacing: 2, textAlign: 'left', padding: '4px 6px', borderBottom: '1px solid rgba(26,48,80,1)', textTransform: 'uppercase' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(weather?.flares ?? []).slice(0, 5).map((flare, i) => (
                    <tr key={i}>
                      <td style={{ padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>
                        <Pill type={flare.classType?.startsWith('X') ? 'red' : flare.classType?.startsWith('M') ? 'amber' : 'cyan'}>{flare.classType}</Pill>
                      </td>
                      <td style={{ fontSize: 11, color: '#7aa5c8', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)', fontFamily: '"Space Mono", monospace' }}>
                        {flare.beginTime ? new Date(flare.beginTime).toISOString().substring(11, 16) : 'N/A'}
                      </td>
                      <td style={{ padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>
                        <Pill type={flare.status === 'ACTIVE' ? 'red' : 'green'}>{flare.status}</Pill>
                      </td>
                    </tr>
                  ))}
                  {(!weather?.flares || weather.flares.length === 0) && (
                    <tr><td colSpan={3} style={{ padding: 12, textAlign: 'center', color: 'rgba(61,98,128,1)', fontSize: 11 }}>Loading space weather…</td></tr>
                  )}
                </tbody>
              </table>
            </Card>
          </div>

          {/* Launch Countdown */}
          <div style={{ background: 'rgba(10,18,32,0.95)', border: '1px solid rgba(0,229,255,0.2)', borderRadius: 6, padding: 12, position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 1, background: 'linear-gradient(90deg,transparent,#00e5ff,transparent)' }} />
            <div style={{ fontFamily: '"Space Mono", monospace', fontSize: 10, color: '#00e5ff', letterSpacing: 3, textTransform: 'uppercase', marginBottom: 10 }}>Next Launch</div>
            <LaunchCountdown />
          </div>

          {/* Missions */}
          <Card title="Active Missions" badge="MULTI-AGENCY" badgeType="multi">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8 }}>
              {[
                { agency: 'NASA',   code: 'NASA',   name: 'Artemis III',     status: 'Pre-launch · 2026 Q3', color: '#1a7fff' },
                { agency: 'ISRO',   code: 'ISRO',   name: 'Chandrayaan-4',   status: 'Development',          color: '#ffa500' },
                { agency: 'SpaceX', code: 'SPACEX', name: 'Starship IFT-8',  status: 'Active · Orbit TBD',   color: '#00e5ff' },
              ].map(m => (
                <div key={m.code} style={{ background: 'rgba(13,24,40,0.8)', border: '1px solid rgba(26,48,80,1)', borderRadius: 4, padding: 8, borderTop: `2px solid ${m.color}` }}>
                  <div style={{ fontSize: 9, letterSpacing: 2, textTransform: 'uppercase', marginBottom: 3, fontFamily: '"Space Mono", monospace', color: m.color }}>{m.agency}</div>
                  <div style={{ fontSize: 12, fontWeight: 600, color: '#c8e0f4', marginBottom: 2 }}>{m.name}</div>
                  <div style={{ fontSize: 10, color: 'rgba(61,98,128,1)' }}>{m.status}</div>
                </div>
              ))}
            </div>
          </Card>

          {/* All Launches Table */}
          {launches.length > 0 && (
            <Card title="All Upcoming Launches" badge={`${launches.length} TOTAL`} badgeType="multi" style={{ padding: 10 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    {['Mission','Agency','Rocket','Site','Window','Status'].map(h => (
                      <th key={h} style={{ fontSize: 9, color: 'rgba(61,98,128,1)', letterSpacing: 2, textAlign: 'left', padding: '4px 6px', borderBottom: '1px solid rgba(26,48,80,1)', textTransform: 'uppercase' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {launches.slice(0, 8).map((l, i) => (
                    <tr key={l.id || i}>
                      <td style={{ fontSize: 12, color: '#c8e0f4', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{l.name}</td>
                      <td style={{ padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>
                        <Pill type={l.agencyCode === 'SPACEX' ? 'cyan' : l.agencyCode === 'ISRO' ? 'amber' : 'green'}>{l.agencyCode}</Pill>
                      </td>
                      <td style={{ fontSize: 11, color: '#7aa5c8', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>{l.rocket}</td>
                      <td style={{ fontSize: 10, color: 'rgba(61,98,128,1)', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>{l.launchSite?.split(',')[0] || 'TBD'}</td>
                      <td style={{ fontSize: 10, color: '#7aa5c8', padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)', fontFamily: '"Space Mono", monospace' }}>
                        {l.windowStart ? new Date(l.windowStart).toISOString().substring(0, 16).replace('T', ' ') : 'TBD'}
                      </td>
                      <td style={{ padding: '5px 6px', borderBottom: '1px solid rgba(13,24,40,1)' }}>
                        <Pill type={l.status === 'Go' ? 'green' : l.status === 'Hold' ? 'red' : 'amber'}>{l.status}</Pill>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          )}
        </div>

        {/* ── NOVA CHAT PANEL ───────────────────────────────────────────── */}
        <div style={{ width: 280, background: 'rgba(8,14,24,0.95)', borderLeft: '1px solid rgba(26,48,80,1)', display: 'flex', flexDirection: 'column', flexShrink: 0 }}>
          <div style={{ padding: 12, borderBottom: '1px solid rgba(26,48,80,1)', display: 'flex', alignItems: 'center', gap: 10 }}>
            <PlanetAvatar size="lg" />
            <div>
              <div style={{ fontFamily: '"Space Mono", monospace', fontSize: 13, color: '#00e5ff', fontWeight: 700, letterSpacing: 2 }}>NOVA</div>
              <div style={{ fontSize: 10, color: wsConnected ? '#00ff9d' : '#ffa500', marginTop: 2 }}>
                {wsConnected ? '● Online · Claude-powered' : '● Connecting…'}
              </div>
            </div>
          </div>
          <NovaChat />
        </div>

      </div>

      <Styles />
    </div>
  )
}

// ─── Small helpers ────────────────────────────────────────────────────────────

function SideSection({ label }) {
  return <div style={{ padding: '6px 14px', fontSize: 10, color: 'rgba(61,98,128,1)', letterSpacing: 3, textTransform: 'uppercase', marginTop: 8 }}>{label}</div>
}

function NavItem({ icon, label, badge, badgeType, active, onClick }) {
  const badgeColors = {
    amber: { bg: '#ffa500', color: '#000' },
    cyan:  { bg: '#00e5ff', color: '#000' },
    default: { bg: '#ff4444', color: '#fff' },
  }
  const bc = badgeColors[badgeType] || badgeColors.default
  return (
    <div onClick={onClick} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px', cursor: 'pointer', borderLeft: `2px solid ${active ? '#00e5ff' : 'transparent'}`, background: active ? 'rgba(0,229,255,0.08)' : 'transparent', transition: 'all 0.2s' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 14, width: 18, textAlign: 'center' }}>{icon}</span>
        <span style={{ fontSize: 13, color: active ? '#00e5ff' : '#7aa5c8', fontWeight: 500, letterSpacing: 1 }}>{label}</span>
      </div>
      {badge && (
        <span style={{ fontFamily: '"Space Mono", monospace', fontSize: 9, background: bc.bg, color: bc.color, padding: '1px 5px', borderRadius: 8, fontWeight: 700 }}>{badge}</span>
      )}
    </div>
  )
}

function Stars() {
  return (
    <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none', zIndex: 0 }}>
      {Array.from({ length: 120 }, (_, i) => {
        const size = Math.random() * 1.8 + 0.4
        return (
          <div key={i} style={{
            position: 'absolute',
            width: size, height: size,
            borderRadius: '50%',
            background: '#fff',
            top: `${Math.random() * 100}%`,
            left: `${Math.random() * 100}%`,
            opacity: Math.random() * 0.6 + 0.1,
            animation: `twinkle ${2 + Math.random() * 4}s ease-in-out ${Math.random() * 4}s infinite`,
          }} />
        )
      })}
    </div>
  )
}

function Styles() {
  return (
    <style>{`
      @import url('https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Rajdhani:wght@300;400;500;600;700&display=swap');
      * { margin:0; padding:0; box-sizing:border-box; }
      body { overflow: hidden; }
      .planet {
        border-radius: 50%;
        background: radial-gradient(circle at 35% 35%, #7b2fff, #2d0a8a);
        box-shadow: 0 0 15px rgba(123,47,255,0.5);
        position: absolute;
        animation: pulsePlanet 3s ease-in-out infinite;
      }
      .moon {
        border-radius: 50%;
        background: radial-gradient(circle at 35% 35%, #00e5ff, #0077aa);
        position: absolute;
        transform-origin: 0 0;
      }
      .glow-ring {
        position: absolute;
        border-radius: 50%;
        border: 1px solid rgba(0,229,255,0.3);
        animation: ringPulse 3s ease-in-out infinite;
      }
      @keyframes pulsePlanet {
        0%,100% { box-shadow: 0 0 15px rgba(123,47,255,0.5); }
        50%      { box-shadow: 0 0 25px rgba(123,47,255,0.8); }
      }
      @keyframes moonOrbit {
        0%   { transform: rotate(0deg)   translateX(var(--orbit-r)) rotate(0deg); }
        100% { transform: rotate(360deg) translateX(var(--orbit-r)) rotate(-360deg); }
      }
      @keyframes ringPulse {
        0%,100% { opacity:0.3; transform:scale(1); }
        50%     { opacity:0.7; transform:scale(1.1); }
      }
      @keyframes blink {
        0%,100% { opacity:1; }
        50%     { opacity:0.4; }
      }
      @keyframes twinkle {
        0%,100% { opacity:0.1; }
        50%     { opacity:0.8; }
      }
    `}</style>
  )
}
