import { useState, useEffect, useRef } from 'react'
import { useNovaStore } from '../store/novaStore'
import { sendNovaMessage } from '../services/websocket'

const CONVERSATION_ID = crypto.randomUUID()

const QUICK_PROMPTS = [
  'What is the ISS doing right now?',
  'Any asteroid threats this week?',
  'SpaceX next launch details?',
  'Explain the current solar flare impact',
  'ISRO upcoming missions?',
]

export default function NovaChat() {
  const messages     = useNovaStore((s) => s.chatMessages)
  const isLoading    = useNovaStore((s) => s.isChatLoading)
  const addMessage   = useNovaStore((s) => s.addChatMessage)
  const setLoading   = useNovaStore((s) => s.setChatLoading)
  const wsConnected  = useNovaStore((s) => s.wsConnected)

  const [input, setInput] = useState('')
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const send = (text) => {
    const msg = text.trim() || input.trim()
    if (!msg) return
    setInput('')
    addMessage({ role: 'user', text: msg, ts: new Date().toISOString() })
    setLoading(true)

    const sent = sendNovaMessage(msg, CONVERSATION_ID)
    if (!sent) {
      // WebSocket not yet connected — fallback message
      setTimeout(() => {
        addMessage({
          role: 'nova',
          text: 'Uplink not established. Please wait for WebSocket connection.',
          ts: new Date().toISOString(),
        })
        setLoading(false)
      }, 800)
    }
  }

  return (
    <>
      {/* Messages */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: 12,
        display: 'flex', flexDirection: 'column', gap: 10,
        scrollbarWidth: 'thin', scrollbarColor: 'rgba(26,48,80,1) transparent'
      }}>
        {messages.map((msg) => (
          <div key={msg.id} style={{
            display: 'flex', gap: 8, alignItems: 'flex-start',
            flexDirection: msg.role === 'user' ? 'row-reverse' : 'row'
          }}>
            <div style={{
              width: 22, height: 22, borderRadius: '50%', flexShrink: 0,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 9, fontWeight: 700,
              background: msg.role === 'nova'
                ? 'rgba(123,47,255,0.3)' : 'rgba(0,229,255,0.15)',
              color: msg.role === 'nova' ? '#9b59ff' : '#00e5ff',
              border: `1px solid ${msg.role === 'nova' ? 'rgba(123,47,255,0.4)' : 'rgba(0,229,255,0.3)'}`,
            }}>
              {msg.role === 'nova' ? 'N' : 'U'}
            </div>
            <div style={{
              padding: '8px 10px', borderRadius: 4, fontSize: 12, lineHeight: 1.5,
              maxWidth: 200,
              background: msg.role === 'nova'
                ? 'rgba(13,24,40,0.9)' : 'rgba(0,229,255,0.08)',
              border: `1px solid ${msg.role === 'nova'
                ? 'rgba(26,48,80,1)' : 'rgba(0,229,255,0.2)'}`,
              color: '#c8e0f4',
            }}>
              {msg.text}
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {isLoading && (
          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
            <div style={{
              width: 22, height: 22, borderRadius: '50%',
              background: 'rgba(123,47,255,0.3)', color: '#9b59ff',
              border: '1px solid rgba(123,47,255,0.4)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 9, fontWeight: 700
            }}>N</div>
            <div style={{
              padding: '10px 12px', borderRadius: 4,
              background: 'rgba(13,24,40,0.9)',
              border: '1px solid rgba(26,48,80,1)',
              display: 'flex', gap: 4
            }}>
              {[0, 0.2, 0.4].map((delay, i) => (
                <div key={i} style={{
                  width: 5, height: 5, borderRadius: '50%',
                  background: 'rgba(120,165,200,0.5)',
                  animation: `typingBounce 1.2s ease-in-out ${delay}s infinite`
                }} />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Quick prompts */}
      <div style={{ padding: '6px 8px', borderTop: '1px solid rgba(26,48,80,1)' }}>
        <div style={{
          fontSize: 9, color: 'rgba(61,98,128,1)', letterSpacing: 2,
          textTransform: 'uppercase', marginBottom: 5
        }}>Quick Prompts</div>
        {QUICK_PROMPTS.slice(0, 4).map((p) => (
          <button key={p} onClick={() => send(p)} style={{
            display: 'block', width: '100%', textAlign: 'left',
            padding: '5px 8px', marginBottom: 3,
            background: 'rgba(13,24,40,0.8)',
            border: '1px solid rgba(26,48,80,1)',
            borderRadius: 3, color: 'rgba(122,165,200,1)', fontSize: 11,
            cursor: 'pointer', fontFamily: 'Rajdhani, sans-serif',
            transition: 'all 0.2s'
          }}>
            ▸ {p}
          </button>
        ))}
      </div>

      {/* Input row */}
      <div style={{
        padding: 10, borderTop: '1px solid rgba(26,48,80,1)',
        display: 'flex', gap: 6
      }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
          placeholder={wsConnected ? 'Ask Nova…' : 'Connecting…'}
          style={{
            flex: 1, background: 'rgba(13,24,40,0.9)',
            border: `1px solid ${wsConnected ? 'rgba(26,48,80,1)' : 'rgba(255,165,0,0.3)'}`,
            borderRadius: 3, padding: '7px 10px',
            color: '#c8e0f4', fontSize: 12,
            fontFamily: 'Rajdhani, sans-serif', outline: 'none'
          }}
        />
        <button onClick={() => send()} style={{
          padding: '7px 10px',
          background: wsConnected ? 'rgba(0,229,255,0.1)' : 'rgba(50,50,60,0.5)',
          border: `1px solid ${wsConnected ? 'rgba(0,229,255,0.3)' : 'rgba(80,80,90,0.3)'}`,
          borderRadius: 3, color: wsConnected ? '#00e5ff' : '#555',
          cursor: wsConnected ? 'pointer' : 'not-allowed', fontSize: 14
        }}>↑</button>
      </div>

      <style>{`
        @keyframes typingBounce {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-5px); }
        }
      `}</style>
    </>
  )
}
