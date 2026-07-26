#
# RaTOS Real-Time OS — EVL base image (layer 1)
#
# Minimal bootable image: EVL kernel + libevl only.
# Use this as the target when developing or testing CoreRaT against EVL.
#
# Build (QEMU):
#   kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/evl.yaml
#
# Build (Odroid H4):
#   kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/target/evl.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS EVL base image — EVL kernel + libevl + reflect-cpp (layer 0)"
HOSTNAME = "ratos-evl"

# Ensure all base packages are in isar-apt before do_rootfs_install runs.
DEPENDS = "linux-xenomai-4 libevl reflect-cpp sshd-regen-keys expand-on-first-boot"

IMAGE_PREINSTALL += " \
    bash-completion vim \
    net-tools iputils-ping ssh \
    dbus"

# libevl: EVL user-space library and headers
# libreflect-cpp-dev: header-only C++20 reflection (needed by everything above)
# sshd-regen-keys: regenerate SSH host keys on first boot (pulls openssh-server)
# expand-on-first-boot: grow the root partition to full medium on first boot
IMAGE_INSTALL += "libevl libreflect-cpp-dev sshd-regen-keys expand-on-first-boot"

# Pull in the full Xenomai 4 / EVL test suite from upstream
IMAGE_INSTALL:append:xenomai4 = " libevl-test"

ROOTFS_FEATURES:remove = "generate-sbom"

require recipes-core/images/ratos-container-qemu-setup.inc
