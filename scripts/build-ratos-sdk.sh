#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "${script_dir}/.." && pwd)

memory_limit=${RATOS_BUILD_MEMORY_LIMIT:-24g}
swap_limit=${RATOS_BUILD_SWAP_LIMIT:-28g}

cd "${repo_dir}"
exec kas-container \
    --runtime-args "--memory=${memory_limit} --memory-swap=${swap_limit}" \
    --isar shell \
    kas.yaml:kas/board/container-amd64.yaml \
    -c "bitbake -c populate_sdk ratos-dev-image"