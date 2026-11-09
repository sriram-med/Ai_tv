# AI TV Gateway

Node.js 22+ dependency-free prototype for GPT-ranked recommendations.

## Setup
1. Copy `.env.example` into your deployment provider's environment variables (do not commit actual secrets).
2. Configure `OPENAI_API_KEY` and a long random `AI_TV_DEVICE_TOKEN`.
3. Run `node server.mjs`.
4. Send HTTPS POST `/v1/recommend` with `Authorization: Bearer <device token>` and JSON body:
   `{"query":"uplifting science fiction","catalog":[{"id":"123","title":"Example","overview":"...","providers":["Example Service"]}]}`

The gateway ranks only titles supplied by the client, preventing fabricated streaming availability. No catalog retrieval, subscription verification, TV client authentication provisioning, or production deployment is included yet.

## Security
Do not put the OpenAI API key in the Android APK. The shared device token is only a prototype and is extractable from a client: production must use short-lived device-scoped tokens issued by a pairing service, TLS, per-device quotas and persistent rate limits. Do not expose the gateway publicly before these are implemented.
