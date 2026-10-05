# Peer Skill-Swap Board

A real-time peer learning platform. Users can post skills they can offer and skills they need, get matched, and join public or private study sessions where they chat live, share reference material and learn together.

**Live demo**
- Frontend: https://peer-skill-swap-1.onrender.com
- Backend API: https://peer-skill-swap.onrender.com

> The app is hosted on Render's free tier, so the first request after a period of inactivity can take a little while to wake up.

## Features

**Skill-swap board**
- Post "can help with X" and "need help with Y"
- Get matched with users whose offers and needs line up
- Receive live notifications when you are matched or someone replies

**Study sessions**
- Browse a board of all study sessions (public and private)
- Create group or one-on-one sessions
- Request to join a session; the host or an admin approves or declines
- Invite people to private or one-on-one sessions
- Leave a session at any time

**Session room**
- Live per-session chat with persistent message history
- Member list with roles (host, admin, member) and host-only admin promotion
- Wikipedia reference panel for looking up concepts without leaving the session
- Pop-up notifications scoped to the sessions you belong to

**General**
- Registration and login (authentication)
- Real-time updates over WebSockets
- Dark mode theme

## Tech Stack

| Layer      | Technology                                  |
|------------|---------------------------------------------|
| Backend    | Java, Spring Boot, REST API, WebSockets     |
| Frontend   | HTML, CSS, JavaScript                       |
| Database   | PostgreSQL                                  |
| Tooling    | Docker, Docker Compose, Git                 |
| Hosting    | Render                                      |
| External   | Wikipedia API (reference resources)         |

## Project Structure

```
.
├── backend/             # Spring Boot app: auth, posts, matching, sessions, chat, notifications
├── frontend/            # HTML/CSS/JS client
├── docker-compose.yml   # Local PostgreSQL service
└── README.md
```

## Getting Started

### Prerequisites

- Java 17+
- Maven or Gradle (or the wrapper in `backend/`)
- Docker and Docker Compose
- A modern web browser

### 1. Clone the repository

```bash
git clone https://github.com/ziyanda-sithole/peer-skill-swap.git
cd peer-skill-swap
```

### 2. Start the database

```bash
docker compose up -d
```

### 3. Run the backend

```bash
cd backend
./mvnw spring-boot:run      # Maven
# or
./gradlew bootRun           # Gradle
```

The API runs on `http://localhost:8080` by default.

### 4. Run the frontend

```bash
cd frontend
python3 -m http.server 3000
```

Open `http://localhost:3000` in your browser, register an account and start exploring.

## Configuration

Database and server settings live in `backend/src/main/resources/application.properties`. Make sure the datasource URL, username and password match your `docker-compose.yml`. In production, supply these through environment variables rather than committing them.

## How It Works

1. A user registers and logs in.
2. They post skills they offer or need, or browse and create study sessions.
3. To join a session, a user sends a join request; the host or an admin approves it. Private and one-on-one sessions can also be joined by invite.
4. Members chat live inside the session room, look up concepts through the Wikipedia panel, and receive pop-up notifications for their sessions.

## Roadmap

- [ ] Session management: delete empty sessions, add or remove members by username, admins can remove members and other admins (never the host)
- [ ] Private messaging: 1:1 and group DMs, replying privately to a session message, member info panel
- [ ] Automated tests and CI pipeline