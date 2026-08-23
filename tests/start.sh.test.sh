#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
test_dir=$(mktemp -d)
cleanup() {
  rm -rf "$test_dir"
}

trap cleanup EXIT

[ -f "$project_root/start.sh" ] || {
  echo "expected start.sh at the repository root" >&2
  exit 1
}

mkdir -p "$test_dir/api" "$test_dir/web" "$test_dir/bin"
cp "$project_root/start.sh" "$test_dir/start.sh"
chmod +x "$test_dir/start.sh"

cat > "$test_dir/api/gradlew" <<'EOF'
#!/bin/sh
echo api-started >> "$TEST_LOG"
echo "$$" > "$TEST_DIR/api-pid"
echo 'Tomcat started on port 18080 (http) with context path '\''/'\'''
while :; do sleep 1; done
EOF
chmod +x "$test_dir/api/gradlew"

cat > "$test_dir/bin/npm" <<'EOF'
#!/bin/sh
echo web-started >> "$TEST_LOG"
echo "$$" > "$TEST_DIR/web-pid"
echo '  ➜  Local:   http://localhost:15173/'
while :; do sleep 1; done
EOF
chmod +x "$test_dir/bin/npm"

cat > "$test_dir/bin/lsof" <<'EOF'
#!/bin/sh
if [ "$1" = '-tiTCP:8080' ]; then
  if [ -f "$TEST_DIR/8080-listening" ]; then
    echo 81080
    exit 0
  fi
  exit 1
fi

if [ "$1" = '-tiTCP:5173' ]; then
  if [ -f "$TEST_DIR/5173-listening" ]; then
    echo 85173
    exit 0
  fi
  exit 1
fi

EOF
chmod +x "$test_dir/bin/lsof"

cat > "$test_dir/bin/kill" <<'EOF'
#!/bin/sh
echo "$*" >> "$TEST_LOG"
case "$*" in
  *81080*) rm -f "$TEST_DIR/8080-listening"; exit 0 ;;
  *85173*) rm -f "$TEST_DIR/5173-listening"; exit 0 ;;
esac
exec /bin/kill "$@"
EOF
chmod +x "$test_dir/bin/kill"

touch "$test_dir/8080-listening" "$test_dir/5173-listening"

(
  cd "$test_dir"
  TEST_DIR="$test_dir" TEST_LOG="$test_dir/events.log" PATH="$test_dir/bin:$PATH" ./start.sh
) > "$test_dir/start-output.log" 2>&1 &
script_pid=$!

for _ in 1 2 3 4 5; do
  [ -f "$test_dir/events.log" ] && grep -q '^api-started$' "$test_dir/events.log" && grep -q '^web-started$' "$test_dir/events.log" && grep -q '^API: http://localhost:18080$' "$test_dir/start-output.log" && grep -q '^Web: http://localhost:15173$' "$test_dir/start-output.log" && break
  sleep 1
done

kill -TERM "$script_pid"
wait "$script_pid" || true

grep -q '81080' "$test_dir/events.log"
grep -q '85173' "$test_dir/events.log"
grep -q '^api-started$' "$test_dir/events.log"
grep -q '^web-started$' "$test_dir/events.log"
grep -q '^API: http://localhost:18080$' "$test_dir/start-output.log"
grep -q '^Web: http://localhost:15173$' "$test_dir/start-output.log"
! grep -q 'Tomcat started on port' "$test_dir/start-output.log"
! grep -q 'Local:.*http://localhost:15173' "$test_dir/start-output.log"

(
  cd "$test_dir"
  TEST_DIR="$test_dir" TEST_LOG="$test_dir/events.log" PATH="$test_dir/bin:$PATH" ./start.sh
) >/dev/null 2>&1 &
script_pid=$!

for _ in 1 2 3 4 5; do
  [ -f "$test_dir/events.log" ] && [ "$(grep -c '^api-started$' "$test_dir/events.log")" -eq 2 ] && [ "$(grep -c '^web-started$' "$test_dir/events.log")" -eq 2 ] && break
  sleep 1
done

kill -TERM "$script_pid"
wait "$script_pid" || true

[ "$(grep -c '^api-started$' "$test_dir/events.log")" -eq 2 ]
[ "$(grep -c '^web-started$' "$test_dir/events.log")" -eq 2 ]
