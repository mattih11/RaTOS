#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "${script_dir}/.." && pwd)

memory_limit=${RATOS_BUILD_MEMORY_LIMIT:-24g}
swap_limit=${RATOS_BUILD_SWAP_LIMIT:-28g}
board=${1:-${RATOS_BUILD_BOARD:-container-amd64}}
variant=${2:-single}

if [[ $# -gt 2 || ! -f "${repo_dir}/kas/board/${board}.yaml" ||
      ( "$variant" != single && "$variant" != ab ) ||
      ( "$variant" == ab && "$board" != odroid-h4 ) ]]; then
    echo "Usage: $0 [board] [single|ab] (ab requires odroid-h4)" >&2
    exit 2
fi

target=musicrat
if [[ "$variant" == ab ]]; then
    target=musicrat-swupdate
fi

cd "${repo_dir}"
kas-container \
    --runtime-args "--memory=${memory_limit} --memory-swap=${swap_limit}" \
    --isar build \
    "kas.yaml:kas/board/${board}.yaml:kas/target/${target}.yaml"

if [[ "$variant" == ab ]]; then
    python3 "${script_dir}/check-musicrat-ab-layout.py" \
        "${repo_dir}/build/tmp/deploy/images/${board}/ratos-musicrat-image-swupdate-ratos-${board}.wic"
fi