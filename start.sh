#!/bin/sh
set -eu

api_pid=''
web_pid=''
api_log=''
web_log=''

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

wait_for_log_port() {
  process_id=$1
  log_file=$2
  port_pattern=$3

  while kill -0 "$process_id" 2>/dev/null; do
    port=$(sed -n "$port_pattern" "$log_file" | tail -n 1)
    [ -z "$port" ] || {
      echo "$port"
      return 0
    }
    sleep 1
  done

  echo "Process $process_id exited before opening a TCP listener." >&2
  exit 1
}

cleanup() {
  trap - EXIT INT TERM

  [ -z "$api_pid" ] || env kill -TERM "$api_pid" 2>/dev/null || true
  [ -z "$web_pid" ] || env kill -TERM "$web_pid" 2>/dev/null || true
  [ -z "$api_pid" ] || wait "$api_pid" 2>/dev/null || true
  [ -z "$web_pid" ] || wait "$web_pid" 2>/dev/null || true
  [ -z "$api_log" ] || rm -f "$api_log"
  [ -z "$web_log" ] || rm -f "$web_log"
}

trap cleanup EXIT INT TERM

stop_port_listener 8080
stop_port_listener 5173

api_log=$(mktemp)
web_log=$(mktemp)

(
  cd api
  exec ./gradlew bootRun > "$api_log" 2>&1
) &
api_pid=$!

(
  cd web
  exec npm run dev > "$web_log" 2>&1
) &
web_pid=$!

api_port=$(wait_for_log_port "$api_pid" "$api_log" 's/.*Tomcat started on port \([0-9][0-9]*\).*/\1/p')
web_port=$(wait_for_log_port "$web_pid" "$web_log" 's|.*http://localhost:\([0-9][0-9]*\)/.*|\1|p')

echo "API: http://localhost:$api_port"
echo "Web: http://localhost:$web_port"

wait "$api_pid"
