FROM node:20-bullseye

# 1. Workdir in the container
WORKDIR /app

# 2. Install Expo CLI + ngrok globally
RUN npm install -g expo@~54.0.0 @expo/ngrok@^4.1.0

# 3. Copy only dependency manifests first (better build caching)
COPY package.json package-lock.json ./

# 4. Install all dependencies inside the image
#    (async-storage will be installed here)
RUN npm ci

# 5. Copy the rest of the app source
COPY . .

# 6. Environment variables for dev / hot reload
ENV CHOKIDAR_USEPOLLING=true \
    WATCHPACK_POLLING=true \
    NODE_ENV=development \
    EXPO_DEVTOOLS_LISTEN_ADDRESS=0.0.0.0 \
    EXPO_NO_TELEMETRY=1

# 7. Ports used by Expo (Metro + devtools)
EXPOSE 19000 19001 19002 8081

# 8. Start Expo in tunnel mode, non-interactive
CMD ["npx", "expo", "start", "--tunnel", "--non-interactive"]
