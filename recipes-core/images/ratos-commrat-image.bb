#
# RaTOS Real-Time OS — CommRaT image (layer 3)
#
# CoreRaT image + CommRaT communication framework.
# Use this as the target when developing end-user applications on top of CommRaT.
#
# Build (QEMU):
#   kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/commrat.yaml
#
# Build (Odroid H4):
#   kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/target/commrat.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS CommRaT image — EVL + CoreRaT + CommRaT (layer 3, for application development)"
HOSTNAME = "ratos-commrat"

DEPENDS = "linux-xenomai-4 libevl reflect-cpp sertial corerat commrat \
           sshd-regen-keys expand-on-first-boot"

IMAGE_PREINSTALL += " \
    bash-completion vim \
    net-tools iputils-ping ssh \
    dbus"

IMAGE_INSTALL += " \
    libevl \
    libreflect-cpp-dev libsertial-dev sertial-tools \
    libcorerat-dev corerat-tools \
    libcommrat-dev \
    sshd-regen-keys expand-on-first-boot"

IMAGE_INSTALL:append:xenomai4 = " libevl-test"

ROOTFS_FEATURES:remove = "generate-sbom"

require recipes-core/images/ratos-container-qemu-setup.inc
