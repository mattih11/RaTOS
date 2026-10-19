#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "${script_dir}/.." && pwd)

memory_limit=${RATOS_BUILD_MEMORY_LIMIT:-24g}
swap_limit=${RATOS_BUILD_SWAP_LIMIT:-28g}

cd "${repo_dir}"
kas-container \
    --runtime-args "--memory=${memory_limit} --memory-swap=${swap_limit}" \
    --isar build \
    kas.yaml:kas/board/odroid-h4.yaml:kas/target/musicrat-installer.yaml

python3 "${script_dir}/check-musicrat-ab-layout.py" \
    "${repo_dir}/build/tmp/deploy/images/odroid-h4/ratos-musicrat-image-swupdate-ratos-odroid-h4.wic"