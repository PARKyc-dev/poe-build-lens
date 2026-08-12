#!/bin/sh

set -eu

if ! command -v luajit >/dev/null 2>&1; then
    printf '%s\n' 'LuaJIT 2.1 is required. Install it before running the PoB spike.'
    exit 1
fi

if ! luajit -e 'require("lfs")' >/dev/null 2>&1; then
    printf '%s\n' 'LuaFileSystem is required. Install the lfs module for LuaJIT 5.1 before running the PoB spike.'
    exit 1
fi

if ! luajit -e 'require("lua-utf8")' >/dev/null 2>&1; then
    printf '%s\n' 'lua-utf8 is required by Path of Building on this host. Install it for LuaJIT 5.1 before running the PoB spike.'
    exit 1
fi

luajit -v 2>&1
printf '%s\n' 'LuaFileSystem OK'
printf '%s\n' 'lua-utf8 OK'
