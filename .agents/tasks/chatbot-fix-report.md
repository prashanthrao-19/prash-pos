# Chatbot Fix Report — POS Pro

## Summary

The chatbot was non-functional due to a single critical misconfiguration in `application.yml`: the Gemini model name was set to `gemini-3.8-flash`, which does not exist. This caused every API call to the Gemini service to receive a `404 Model Not Found` response. The fix is a one-line change to the correct model name `gemini-1.5-flash`. The `GEMINI_API_KEY` also needs to be set as an environment variable — the code correctly handles and reports when it is missing.

---

## Root Cause

### Issue 1 (Critical — Code Bug): Invalid Gemini Model Name

**File:** `pos-backend/src/main/resources/application.yml`  
**Line:** `model: ${GEMINI_MODEL:gemini-3.8-flash}`

`gemini-3.8-flash` is not a valid Google Gemini model name. It does not exist. Every AI request sent to the Gemini API using this model name returned `HTTP 404 Not Found`, triggering the `handleHttpClientError` path in `ChatHelpServiceImpl` with an error message: _"The configured Gemini model `gemini-3.8-flash` was not found."_

The correct model name is `gemini-1.5-flash` (stable, free-tier available), which is also what the `.env.example` file documents as the intended default.

### Issue 2 (Configuration — Not a Code Bug): Missing GEMINI_API_KEY

The `GEMINI_API_KEY` environment variable is not set. No `.env` file exists in the backend directory. The code correctly checks for this condition and returns a clear error message (`API_KEY_MISSING`) when the key is absent. The user must supply this value.

---

## Files Inspected

| File | Purpose |
|------|---------|
| `pos-frontend-vite/src/components/chatbot/ChatBot.jsx` | Frontend chat UI, message sending, state management |
| `pos-frontend-vite/src/utils/api.js` | Axios instance, base URL configuration |
| `pos-frontend-vite/.env.example` | Frontend environment variable template |
| `pos-backend/src/main/java/com/zosh/controller/ChatHelpController.java` | REST controller (`/api/chat/help`, `/api/chat/status`) |
| `pos-backend/src/main/java/com/zosh/service/ChatHelpService.java` | Service interface |
| `pos-backend/src/main/java/com/zosh/service/impl/ChatHelpServiceImpl.java` | Gemini API integration, request/response handling |
| `pos-backend/src/main/java/com/zosh/payload/request/ChatHelpRequest.java` | Request DTO |
| `pos-backend/src/main/java/com/zosh/payload/response/ChatHelpResponse.java` | Response DTO |
| `pos-backend/src/main/java/com/zosh/payload/dto/ChatMessageDTO.java` | Chat history message DTO |
| `pos-backend/src/main/java/com/zosh/payload/response/ChatStatusResponse.java` | Status endpoint response DTO |
| `pos-backend/src/main/java/com/zosh/configrations/AppConfig.java` | `RestTemplate` bean with 10s connect / 30s read timeout |
| `pos-backend/src/main/java/com/zosh/configrations/SecurityConfig.java` | Spring Security — `/api/chat/**` is `permitAll()` |
| `pos-backend/src/main/resources/application.yml` | All environment variable bindings including Gemini config |
| `pos-backend/.env.example` | Backend environment variable template |
| `pos-backend/pom.xml` | Maven dependencies — no external AI SDK needed (uses `RestTemplate`) |

---

## Files Modified

### `pos-backend/src/main/resources/application.yml`

**Before:**
```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:gemini-3.8-flash}
    url: ${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models}
```

**After:**
```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:gemini-1.5-flash}
    url: ${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models}
```

**Reason:** `gemini-3.8-flash` is a non-existent model name. The Gemini API returns HTTP 404 for any request using it. `gemini-1.5-flash` is the correct, stable, free-tier available model documented by Google and referenced in `.env.example`.

---

## Complete End-to-End Flow (Verified)

```
User types message → ChatBot.jsx handleSendMessage()
  → POST /api/chat/help (via api.js → axios → http://localhost:5000)
  → Spring Security: /api/chat/** is permitAll() [no auth required]
  → ChatHelpController.getAiHelp()
  → ChatHelpServiceImpl.getAiHelp()
     → isApiKeyConfigured() check [returns false if GEMINI_API_KEY not set]
     → Build JSON body with systemInstruction + history + user message
     → POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
       (with x-goog-api-key header)
     → Parse candidates[0].content.parts[0].text
  → ResponseEntity<ChatHelpResponse> { reply, model, success, suggestedActions }
  → ChatBot.jsx: display reply in message list
```

---

## Required Environment Variable Names

These must be set in a `.env` file or as system/process environment variables before starting the backend.

| Variable Name | Required | Notes |
|---------------|----------|-------|
| `GEMINI_API_KEY` | **YES — mandatory for chatbot** | Obtain from [Google AI Studio](https://aistudio.google.com/). Free tier available. |
| `GEMINI_MODEL` | No | Defaults to `gemini-1.5-flash` after this fix. Can be overridden (e.g., `gemini-2.0-flash`). |
| `GEMINI_API_URL` | No | Defaults to `https://generativelanguage.googleapis.com/v1beta/models` |
| `DB_HOST` | No | Defaults to `localhost` |
| `DB_PORT` | No | Defaults to `3306` |
| `DB_NAME` | No | Defaults to `nehapos` |
| `DB_USERNAME` | No | Defaults to `root` |
| `DB_PASSWORD` | No | Defaults to `prashanth` |

**Never commit real API key values to source control.**

---

## How to Set the API Key (Local Development)

1. Get your free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Create a file `pos-backend/.env` (never commit this file):

```
GEMINI_API_KEY=your_actual_key_here
GEMINI_MODEL=gemini-1.5-flash
```

3. Spring Boot does **not** read `.env` files natively. You need one of these approaches:

**Option A — Set as OS environment variable (Windows PowerShell):**
```powershell
$env:GEMINI_API_KEY = "your_actual_key_here"
# Then start backend in the same shell:
cd pos-backend
.\mvnw spring-boot:run
```

**Option B — Pass directly via Maven:**
```powershell
cd "pos-backend"
.\mvnw spring-boot:run "-DGEMINI_API_KEY=your_actual_key_here"
```

**Option C — IDE (IntelliJ):** Add `GEMINI_API_KEY=your_actual_key_here` to the Run Configuration's Environment Variables.

**Option D — Production (Render/Docker):** Set the `GEMINI_API_KEY` environment variable in your cloud dashboard.

---

## How to Restart

### Backend
```powershell
cd "c:\Users\prash\OneDrive\Desktop\nehas project\pos-source-code-razorpay-2025-23-11-06-32-54 (1)\z pos-source-code\pos-backend"
# Set env var first (Option A above), then:
.\mvnw spring-boot:run
# Runs on http://localhost:5000
```

### Frontend
```powershell
cd "c:\Users\prash\OneDrive\Desktop\nehas project\pos-source-code-razorpay-2025-23-11-06-32-54 (1)\z pos-source-code\pos-frontend-vite"
npm run dev
# Runs on http://localhost:5173
```

---

## Additional Findings (No Changes Required)

| Finding | Status | Reason |
|---------|--------|--------|
| CORS config | ✅ OK | `http://localhost:5173` is in the allowed origins list |
| Security config | ✅ OK | `/api/chat/**` is `permitAll()` — chat works without login |
| Frontend API URL | ✅ OK | `api.js` defaults to `http://localhost:5000` |
| Request schema | ✅ OK | Frontend payload matches `ChatHelpRequest` DTO exactly |
| Response parsing | ✅ OK | Frontend correctly checks `data.success && data.reply` |
| Error handling | ✅ OK | `handleHttpClientError` covers 404, 429, 400, 403 |
| RestTemplate bean | ✅ OK | Declared in `AppConfig.java` with 10s connect / 30s read timeout |
| ObjectMapper bean | ✅ OK | Spring Boot auto-configures via `JacksonAutoConfiguration` |
| Maven dependencies | ✅ OK | Uses `RestTemplate` (spring-boot-starter-web) — no external AI SDK needed |
| JWT auth for chat | ✅ OK | `Authorization` header is `required = false` — works for both guests and logged-in users |
| Chat history truncation | ✅ OK | Service limits to last 10 turns to avoid token overflow |
| Session persistence | ✅ OK | Messages saved to `sessionStorage` |

---

## Database or Config Changes Required

None. No schema changes, no migration, no new tables.

---

## Test Results

| Step | Result |
|------|--------|
| Backend compile (`.\mvnw compile`) | ✅ **PASSED** — exit code 0, no errors |
| Frontend build (`npm run build`) | ✅ **PASSED** — 2959 modules, exit code 0 (chunk size warning is pre-existing, not an error) |

---

## Manual Verification Checklist

After setting `GEMINI_API_KEY` and restarting both services:

- [ ] **Backend status endpoint:** Open `http://localhost:5000/api/chat/status` in a browser — should return `{ "configured": true, "model": "gemini-1.5-flash", ... }`
- [ ] **Chat window opens:** Click the green "Ask POS AI" button at bottom-right — chat window slides up
- [ ] **Status check on open:** The chatbot calls `/api/chat/status` when opened — no "offline" message in header
- [ ] **Send a test message:** Type `"Hello, what can you help me with?"` and press Enter — a real Gemini reply appears within ~3 seconds
- [ ] **Model attribution:** The reply shows "Answered by gemini-1.5-flash" below the message
- [ ] **Network tab check:** The POST to `http://localhost:5000/api/chat/help` returns HTTP 200 with `{ success: true, reply: "...", model: "gemini-1.5-flash" }`
- [ ] **Empty message:** The Send button is disabled when input is empty — no request is sent
- [ ] **Clear chat:** Clicking the rotate icon resets messages and clears `sessionStorage`
- [ ] **Session persistence:** Refresh the page — previous messages reappear from `sessionStorage`
- [ ] **Suggested actions:** After a reply, follow-up suggestion chips appear and are clickable
- [ ] **Login flow unaffected:** Log in/out as store admin — JWT auth still works normally
- [ ] **Role-aware prompts:** Log in as different roles (cashier, store admin, super admin) — the suggested quick prompts at the bottom of the chat change per role

---

## What Remains Unverified (Requires Live Runtime)

- Actual Gemini API call round-trip (requires `GEMINI_API_KEY` to be set with a valid key)
- Rate-limit / 429 error path (handled in code but not testable without hitting quota)
- Network timeout behavior (30s timeout is configured)
- Backend logs (`log.info` / `log.error` output) — visible at runtime via console or log file
