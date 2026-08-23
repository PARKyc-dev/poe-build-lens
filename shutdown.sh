#!/bin/sh
set -eu

stop_port_listener() {
  port=$1
  pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)

  [ -n "$pids" ] || return 0

  echo "Stopping process on port $port: $pids"
  env kill $pids

  attempts=0
  while lsof -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [ "$attempts" -eq 5 ]; then
      echo "Port $port is still in use after stopping its listener." >&2
      exit 1
    fi
    sleep 1
  done
}

stop_port_listener 8080
stop_port_listener 5173
