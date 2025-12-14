# OpenHand – MANA Mobile Application

Mobile application for *Maison d’Accueil des Nouveaux Arrivants (MANA)* built with Expo, React Native, TypeScript, and Docker.  
This project is part of the Champlain College Systems Development Final Project.

---

## Features

- Home page with MANA branding, banners, announcements, and services  
- Expo Router navigation structure  
- Screens for events, authentication, and language settings  
- Responsive layout for both iOS and Android  
- Docker setup allowing the mobile app, backend, and database to run consistently across all developer machines  
- WhatsApp contact button with automatic deep-linking  
- Modular folder structure for scalable future development

---

## Tech Stack

- React Native (Expo)
- Expo Router
- TypeScript
- Docker & Docker Compose
- PostgreSQL (via Docker)
- Backend (Spring Boot / Node.js)

---

## How to Run (Docker)

```sh
docker compose up --build
```

## Connecting from a phone (Expo Go)

1. Put your phone and laptop on the same Wi‑Fi.
2. Find your laptop's LAN IP (Mac: `ipconfig getifaddr en0`, Linux: `hostname -I | awk '{print $1}'`, Windows: `ipconfig`).
3. Set `EXPO_PUBLIC_API_URL=http://<your-ip>:8080/api` in your `.env` file (used by `docker compose`).
4. Restart the mobile container so Expo picks up the new URL: `docker compose up -d --build mobile-app`.
5. Watch the Metro logs for `[API] Using base URL:` to confirm the app is talking to the right backend.
