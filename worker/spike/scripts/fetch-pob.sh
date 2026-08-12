#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
spike_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
repo_root=$(CDPATH= cd -- "$spike_dir/../.." && pwd)

. "$spike_dir/pob.lock"

cache_dir="$repo_root/worker/spike/.cache"
pob_dir="$cache_dir/PathOfBuilding"

if [ ! -d "$pob_dir" ]; then
    mkdir -p "$cache_dir"
    git clone --depth 1 --branch "$POB_TAG" "$POB_REPOSITORY" "$pob_dir"
fi

actual_commit=$(git -C "$pob_dir" rev-parse HEAD)
if [ "$actual_commit" != "$POB_COMMIT" ]; then
    printf '%s\n' "PoB checkout commit mismatch: expected $POB_COMMIT, got $actual_commit"
    exit 1
fi

for required_path in \
    src/HeadlessWrapper.lua \
    src/Launch.lua \
    LICENSE.md \
    spec/TestBuilds/3.13/OccVortex.lua \
    'spec/TestBuilds/3.13/Mirage Archer Toxic Rain.lua'; do
    if [ ! -f "$pob_dir/$required_path" ]; then
        printf '%s\n' "PoB checkout is missing required file: $required_path"
        exit 1
    fi
done

printf '%s\n' "PoB $POB_TAG is ready at $pob_dir"
