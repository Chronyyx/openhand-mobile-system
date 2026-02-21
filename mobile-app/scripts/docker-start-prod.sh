#!/usr/bin/env bash
# Ensure strict mode; fall back when pipefail is unavailable.
set -euo pipefail 2>/dev/null || set -euo

cd /app

deps_hash_file="node_modules/.openhand-deps-hash"

compute_hash() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum package.json package-lock.json 2>/dev/null | sha256sum | awk '{print $1}'
  else
    # Fallback: best-effort timestamp-based hash
    stat -c '%Y' package.json package-lock.json 2>/dev/null | awk '{print $1"-"$2}'
  fi
}

ensure_deps() {
  local current_hash
  current_hash="$(compute_hash)"

  if [[ ! -d node_modules ]]; then
    echo "[mobile-app] node_modules missing; installing dependencies..."
    npm install
    echo "$current_hash" > "$deps_hash_file"
    return
  fi

  if [[ ! -f "$deps_hash_file" ]]; then
    echo "[mobile-app] deps hash missing; installing dependencies..."
    npm install
    echo "$current_hash" > "$deps_hash_file"
    return
  fi

  local previous_hash
  previous_hash="$(cat "$deps_hash_file" || true)"
  if [[ "$previous_hash" != "$current_hash" ]]; then
    echo "[mobile-app] package.json/package-lock.json changed; installing dependencies..."
    npm install
    echo "$current_hash" > "$deps_hash_file"
  fi
}

ensure_deps

export CI=1

if [ -z "${EXPO_NGROK_AUTHTOKEN:-}" ]; then
  echo "ERROR: EXPO_NGROK_AUTHTOKEN is not set or empty. Please ensure it is passed correctly."
  exit 1
fi
ngrok config add-authtoken "$EXPO_NGROK_AUTHTOKEN"

# Wait for Expo to be ready
export EXPO_PACKAGER_PROXY_URL="https://wes-chromophotographic-boyce.ngrok-free.dev"
npx expo start &

sleep 5

# Start Ngrok static domain on port 8081
exec ngrok http --domain=wes-chromophotographic-boyce.ngrok-free.dev 8081
