FROM node:20-bullseye

WORKDIR /app

RUN npm install -g expo@~54.0.0 @expo/ngrok@^4.1.0

COPY package*.json ./
RUN npm install

COPY . .

ENV CHOKIDAR_USEPOLLING=true \
    WATCHPACK_POLLING=true \
    NODE_ENV=development \
    EXPO_DEVTOOLS_LISTEN_ADDRESS=0.0.0.0 \
    EXPO_NO_TELEMETRY=1

EXPOSE 19000 19001 19002 8081

# Run Expo in tunnel + non-interactive (no questions, just QR)
CMD ["npx", "expo", "start", "--tunnel", "--non-interactive"]
