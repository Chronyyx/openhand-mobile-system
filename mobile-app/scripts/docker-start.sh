#!/usr/bin/env bash
set -euo pipefail

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

exec npx expo start --tunnel --non-interactive

