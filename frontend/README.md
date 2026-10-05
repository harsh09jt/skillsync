# SkillSync frontend

SkillSync is a responsive React dashboard for tracking skills, taking self-assessments, reviewing skill gaps, and finding learning resources. It uses Vite, Tailwind CSS, and custom styling.

## Run locally

```sh
npm install
npm run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). Sign in or register to load live workspace data from the Spring Boot API.

## Backend connection

The API defaults to `http://localhost:8080`. To use another API origin, add a `.env.local` file:

```sh
VITE_API_BASE_URL=http://localhost:8080
```

The frontend uses `POST /api/auth/login` and `POST /api/auth/register`. Keep the backend and its database running to create accounts or sign in.

The backend requires a MySQL database and uses `SKILLSYNC_JWT_SECRET` for production JWT signing. Set `SKILLSYNC_CORS_ALLOWED_ORIGINS` to a comma-separated list of trusted frontend origins when deploying (for example, `https://app.example.com`); the localhost pattern is only the development default. Public registration creates Employee accounts; an Admin assigns SME, Manager, or Admin access.

## AI assistant

After signing in, open **AI assistant** in the sidebar or visit `/assistant`. The authenticated `POST /api/assistant/analyze` endpoint reads only the current user's skills and assessment history, then compares them with configured job-role requirements through existing backend services. Readiness is calculated by the backend; the assistant never receives database credentials.

Optional model-backed request-intent routing uses Gemini's OpenAI-compatible Chat Completions API. Add your Google AI Studio key to the ignored `backend/.env` file:

```properties
GEMINI_API_KEY=your-key
GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
GEMINI_MODEL=gemini-3.1-flash-lite
```

Restart the backend after editing that file. Without a key, local intent routing and the evidence-based gap, career and roadmap analysis still work. The API model only classifies the request; skill levels and readiness are always computed from SkillSync data.

## Checks

```sh
npm run lint
npm run build
```
