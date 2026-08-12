#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)

python3 -m pip install -r "$repo_root/worker/requirements.txt"
cd "$repo_root"
exec python3 -m uvicorn worker.inspect_app:app --port 8000
