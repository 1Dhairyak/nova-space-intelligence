# Nova Space Intelligence System

Full-stack real-time space dashboard — Spring Boot + React + WebSocket + Redis + Claude API.

---

## Architecture

```
React (Vite)          Spring Boot (8080)         External APIs
┌──────────────┐      ┌──────────────────┐       ┌────────────────────┐
│  Zustand     │◄────►│  REST /api/space │◄─────►│  NASA (APOD/NEO/  │
│  Store       │      │                  │       │  DONKI)            │
│              │      │  WebSocket /ws   │◄─────►│  Open Notify (ISS) │
│  SockJS +    │◄────►│  STOMP broker    │       │  SpaceX v4 API     │
│  STOMP       │      │                  │◄─────►│  ISRO Community    │
│              │      │  @Scheduled jobs │◄─────►│  The Space Devs    │
│  NovaChat    │◄────►│  NovaAiService   │◄─────►│  Anthropic Claude  │
└──────────────┘      │                  │       └────────────────────┘
                      │  Redis Cache     │
                      │  MySQL (JPA)     │
                      └──────────────────┘
```

### WebSocket Data Flow

```
External API → @Scheduled Job → Redis (TTL) → STOMP /topic/iss
                                            → STOMP /topic/launches
                                            → STOMP /topic/neo
                                            → STOMP /topic/weather
                                            → STOMP /topic/brief

React Client → /app/nova/chat → NovaChatController
                              → NovaAiService (Redis brief + Claude)
                              → /user/{id}/queue/nova → React
```

---

## Prerequisites

- Java 17+
- Node 20+
- MySQL 8+ running on localhost:3306
- Redis running on localhost:6379
- NASA API key (free at https://api.nasa.gov)
- Anthropic API key

---

## Backend Setup

```bash
cd backend

# Set environment variables (or edit application.yml directly)
export NASA_API_KEY=your_nasa_key
export ANTHROPIC_API_KEY=your_anthropic_key
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your-256-bit-secret-change-this

# Run
./mvnw spring-boot:run
```

The backend:
- Creates the `nova_space` MySQL database automatically on first run
- Connects to Redis at localhost:6379
- Starts scheduled jobs immediately (ISS every 5s, weather every 5min, launches every 5min, NEO every 1hr)
- WebSocket available at ws://localhost:8080/ws

---

## Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at http://localhost:5173 and proxies:
- `/api/*` → `http://localhost:8080`
- `/ws/*`  → `http://localhost:8080` (WebSocket)

---

## API Keys Required

| Service         | Key Required | Get It At                                  | Free Tier        |
|-----------------|-------------|--------------------------------------------|------------------|
| NASA            | Yes         | https://api.nasa.gov                       | 1000 req/hr      |
| Anthropic       | Yes         | https://console.anthropic.com              | Pay per token    |
| SpaceX          | No          | Public API — no key needed                 | Unlimited        |
| ISRO Community  | No          | https://isro.vercel.app — no key needed   | Unlimited        |
| The Space Devs  | Optional    | https://thespacedevs.com/api               | 15 req/hr free   |
| Open Notify ISS | No          | http://api.open-notify.org — no key       | Unlimited        |
| NOAA Kp Index   | No          | https://services.swpc.noaa.gov — no key  | Unlimited        |

---

## WebSocket Topics (subscribe from frontend)

| Topic                       | Payload              | Frequency  |
|-----------------------------|----------------------|------------|
| `/topic/iss`                | IssPosition          | Every 5s   |
| `/topic/launches`           | Launch[]             | Every 5min |
| `/topic/neo`                | NearEarthObject[]    | Every 1hr  |
| `/topic/weather`            | SpaceWeather         | Every 5min |
| `/topic/brief`              | SpaceBriefCache      | Every 5min |
| `/user/queue/nova`          | Nova AI response     | On demand  |

## Send to Backend

| Destination       | Body                                              |
|-------------------|---------------------------------------------------|
| `/app/nova/chat`  | `{ "message": "...", "conversationId": "uuid" }` |

---

## REST Endpoints

```
POST /api/auth/register   → { email, password, displayName }
POST /api/auth/login      → { email, password }
POST /api/auth/refresh    → { refreshToken }
GET  /api/auth/me

GET  /api/space/brief     → Full SpaceBriefCache from Redis
GET  /api/space/iss       → Current ISS position
GET  /api/space/launches  → All launches (SpaceX + ISRO + NASA)
GET  /api/space/neo       → Near Earth Objects (7-day feed)
GET  /api/space/weather   → Kp index + solar flares
GET  /api/space/apod      → Astronomy Picture of the Day
```

---

## Project Structure

```
nova/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/nova/
│       ├── NovaApplication.java
│       ├── config/
│       │   ├── WebSocketConfig.java          ← SockJS + STOMP
│       │   ├── WebSocketAuthChannelInterceptor.java
│       │   ├── SecurityConfig.java           ← JWT filter chain
│       │   ├── RedisConfig.java              ← Per-cache TTL policies
│       │   ├── ApplicationConfig.java        ← Auth beans
│       │   └── HttpClientConfig.java         ← OkHttp + Jackson
│       ├── security/
│       │   ├── JwtService.java               ← Token gen/validation
│       │   └── JwtAuthFilter.java            ← Per-request filter
│       ├── model/
│       │   ├── User.java                     ← JPA entity
│       │   └── SpaceBriefCache.java          ← Redis cache POJO
│       ├── repository/
│       │   └── UserRepository.java
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── NovaAiService.java            ← Claude API middleware
│       │   └── api/
│       │       ├── NasaApiService.java       ← APOD + NEO + DONKI
│       │       ├── SpaceXApiService.java     ← SpaceX v4
│       │       ├── IsroApiService.java       ← ISRO + Space Devs
│       │       └── IssTrackerService.java    ← Open Notify + NOAA Kp
│       ├── scheduler/
│       │   └── SpaceBriefScheduler.java      ← All @Scheduled jobs
│       └── controller/
│           ├── AuthController.java
│           ├── SpaceDataController.java      ← REST endpoints
│           └── NovaChatController.java       ← @MessageMapping
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx                           ← Root, all layout
        ├── store/
        │   └── novaStore.js                  ← Zustand global state
        ├── services/
        │   ├── websocket.js                  ← SockJS + STOMP client
        │   └── api.js                        ← Axios + JWT interceptor
        ├── hooks/
        │   └── useSpaceData.js               ← Initial load + WS connect
        └── components/
            ├── ISSMap.jsx                    ← Canvas orbital map
            ├── LaunchCountdown.jsx           ← Live countdown
            └── NovaChat.jsx                  ← AI chat panel
```
