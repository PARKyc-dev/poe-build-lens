#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT

[ -f "$project_root/shutdown.sh" ] || {
  echo "expected shutdown.sh at the repository root" >&2
  exit 1
}

mkdir -p "$test_dir/bin"
cp "$project_root/shutdown.sh" "$test_dir/shutdown.sh"
chmod +x "$test_dir/shutdown.sh"

cat > "$test_dir/bin/lsof" <<'EOF'
#!/bin/sh
case "$1" in
  *8080*)
    [ -f "$TEST_DIR/8080-listening" ] && echo 81080 && exit 0
    exit 1
    ;;
  *5173*)
    [ -f "$TEST_DIR/5173-listening" ] && echo 85173 && exit 0
    exit 1
    ;;
esac
EOF
chmod +x "$test_dir/bin/lsof"

cat > "$test_dir/bin/kill" <<'EOF'
#!/bin/sh
echo "$*" >> "$TEST_LOG"
case "$*" in
  *81080*) rm -f "$TEST_DIR/8080-listening"; exit 0 ;;
  *85173*) rm -f "$TEST_DIR/5173-listening"; exit 0 ;;
esac
exit 1
EOF
chmod +x "$test_dir/bin/kill"

touch "$test_dir/8080-listening" "$test_dir/5173-listening"

TEST_DIR="$test_dir" TEST_LOG="$test_dir/events.log" PATH="$test_dir/bin:$PATH" "$test_dir/shutdown.sh"

grep -q '81080' "$test_dir/events.log"
grep -q '85173' "$test_dir/events.log"
[ ! -e "$test_dir/8080-listening" ]
[ ! -e "$test_dir/5173-listening" ]

TEST_DIR="$test_dir" TEST_LOG="$test_dir/events.log" PATH="$test_dir/bin:$PATH" "$test_dir/shutdown.sh"
