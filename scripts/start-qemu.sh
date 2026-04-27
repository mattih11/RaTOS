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

# Use KVM if the device node is accessible.
ACCEL="tcg"
if [ -w /dev/kvm ]; then
    ACCEL="kvm"
fi

exec qemu-system-x86_64 \
    -cpu qemu64 \
    -smp 4 \
    -m 2G \
    -machine q35,accel=${ACCEL} \
    -kernel "${KERNEL_FILE}" \
    -initrd "${INITRD_FILE}" \
    -drive file="${EXT4_FILE}",discard=unmap,if=none,id=disk,format=raw \
    -device ide-hd,drive=disk \
    -append "root=/dev/sda rw rootwait console=ttyS0,115200 console=tty0" \
    -serial mon:stdio \
    -netdev user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
    -device virtio-net-pci,netdev=net \
    -nographic \
    "$@"
