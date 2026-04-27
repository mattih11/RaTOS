#!/bin/sh
#
# RaTOS Real-Time OS — start the developer image in QEMU
#
# Boots the ratos-dev-image-container-amd64 ext4 image using the kernel and
# initrd from the ISAR deploy directory. A serial console is attached to
# stdio so the VM shell appears directly in the calling terminal.
#
# Usage: ./scripts/start-qemu.sh [extra QEMU options]
#
# SSH is forwarded on localhost:22222 → VM:22 for convenience:
#   ssh root@localhost -p 22222   (password: root)
#
# KVM is used when available; falls back to TCG emulation automatically.
#
# SPDX-License-Identifier: GPL-3.0-or-later

set -e

SCRIPT_DIR=$(readlink -f "$(dirname "$0")")
BASE_DIR=$(readlink -f "${SCRIPT_DIR}/..")
DEPLOY="${BASE_DIR}/build/tmp/deploy/images/container-amd64"

# Resolve image files — pick the most recently modified if multiple match.
EXT4_FILE=$(ls -t "${DEPLOY}"/ratos-dev-image*container-amd64.ext4 2>/dev/null | head -1)
KERNEL_FILE=$(ls -t "${DEPLOY}"/ratos-dev-image*container-amd64*vmlinuz 2>/dev/null | head -1)
INITRD_FILE=$(ls -t "${DEPLOY}"/ratos-dev-image*container-amd64*initrd.img 2>/dev/null | head -1)

if [ -z "${EXT4_FILE}" ] || [ -z "${KERNEL_FILE}" ] || [ -z "${INITRD_FILE}" ]; then
    echo "ERROR: one or more build artifacts not found under ${DEPLOY}"
    echo "  ext4:   ${EXT4_FILE:-<missing>}"
    echo "  kernel: ${KERNEL_FILE:-<missing>}"
    echo "  initrd: ${INITRD_FILE:-<missing>}"
    echo
    echo "Build the image first:"
    echo "  kas-container --isar build kas.yaml:kas/board/container-amd64.yaml"
    exit 1
fi

echo "ext4:   ${EXT4_FILE}"
echo "kernel: ${KERNEL_FILE}"
echo "initrd: ${INITRD_FILE}"
echo

# Align with upstream xenomai-images/start-qemu.sh:
#   - KVM via -cpu host -enable-kvm; fall back to TCG with -cpu qemu64
#   - Single console=ttyS0 only — systemd-getty-generator auto-starts
#     serial-getty@ttyS0 when ttyS0 is the sole (and therefore primary)
#     console=. Adding console=tty0 as well makes tty0 /dev/console and
#     sends all systemd output to the VGA device, which is invisible with
#     -nographic, causing a silent hang.
if [ -w /dev/kvm ]; then
    CPU_ARGS="-cpu host -enable-kvm"
else
    CPU_ARGS="-cpu qemu64"
fi

exec qemu-system-x86_64 \
    ${CPU_ARGS} \
    -smp 4 \
    -m 2G \
    -machine q35 \
    -kernel "${KERNEL_FILE}" \
    -initrd "${INITRD_FILE}" \
    -drive file="${EXT4_FILE}",discard=unmap,if=none,id=disk,format=raw \
    -device ide-hd,drive=disk \
    -append "root=/dev/sda rw rootwait console=ttyS0" \
    -serial mon:stdio \
    -netdev user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
    -device virtio-net-pci,netdev=net \
    -nographic \
    "$@"
