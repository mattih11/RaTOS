#
# RaTOS Real-Time OS — CoreRaT image (layer 2)
#
# EVL base image + CoreRaT platform library.
# Use this as the target when developing or testing CommRaT against CoreRaT.
#
# Build (QEMU):
#   kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/corerat.yaml
#
# Build (Odroid H4):
#   kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/target/corerat.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS CoreRaT image — EVL + CoreRaT (layer 2, for CommRaT development)"
HOSTNAME = "ratos-corerat"

# Ensure all required packages are in isar-apt before do_rootfs_install runs.
DEPENDS = "linux-xenomai-4 libevl reflect-cpp sertial corerat \
           sshd-regen-keys expand-on-first-boot"

IMAGE_PREINSTALL += " \
    bash-completion vim \
    net-tools iputils-ping ssh \
    dbus"

# libevl: EVL user-space library and headers
# libreflect-cpp-dev / libsertial-dev / sertial-tools: serialization stack
# libcorerat-dev / corerat-tools: CoreRaT headers, link stubs, and CLI tools
IMAGE_INSTALL += " \
    libevl \
    libreflect-cpp-dev libsertial-dev sertial-tools \
    libcorerat-dev corerat-tools \
    sshd-regen-keys expand-on-first-boot"

IMAGE_INSTALL:append:xenomai4 = " libevl-test"

ROOTFS_FEATURES:remove = "generate-sbom"

require recipes-core/images/ratos-container-qemu-setup.inc
