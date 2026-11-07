# API reference
All endpoints are under /api. Authenticated routes require Authorization: Bearer <JWT>.

- POST /auth/register — {name,email,password}; password minimum 10 characters.
- POST /auth/login — {email,password}.
- GET /me — current account.
- GET /devices — current user's devices and latest shared location.
- POST /devices — {name,imei,brand?,model?}; IMEI must pass 15-digit Luhn validation.
- POST /devices/:id/heartbeat — {battery?}.
- POST /devices/:id/location — {latitude,longitude,accuracy?,battery?,source?}.
- GET /devices/:id/locations?limit=50 — history, maximum 250.
- PATCH /devices/:id/lost-mode — {enabled:boolean}.
- POST /devices/:id/unlink — unlink.
- GET /notifications — account notifications.
- POST /notifications/:id/read — mark read.
- DELETE /account — delete account and associated records.
- GET /healthz — deployment health.

All location writes require a valid owner JWT and are scoped to that owner's device. The current Android project is a UI/service scaffold only; authenticated app linking and location-upload wiring remain TODOs. Before public launch, implement revocable per-device credentials, refresh-token rotation, verified email, abuse monitoring and end-to-end mobile integration tests.