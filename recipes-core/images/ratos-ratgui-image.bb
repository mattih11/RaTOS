#
# RaTOS Real-Time OS — RatGUI image (layer 3)
#
# CommRaT image + RatGUI graphical interface.
# Use this as the target when developing or testing RatGUI applications.
#
# Build (QEMU):
#   kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/ratgui.yaml
#
# Build (Odroid H4):
#   kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/target/ratgui.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS RatGUI image — EVL + SeRTial + CoreRaT + CommRaT + RatGUI (layer 3)"
HOSTNAME = "ratos-ratgui"

# TODO: add ratgui to DEPENDS when the recipe exists.
DEPENDS = "linux-xenomai-4 libevl reflect-cpp sertial corerat commrat \
           sshd-regen-keys expand-on-first-boot"

IMAGE_PREINSTALL += " \
    bash-completion vim \
    net-tools iputils-ping ssh \
    rsync cmake \
    dbus"

IMAGE_INSTALL += " \
    libevl \
    libreflect-cpp-dev libsertial-dev sertial-tools \
    libcorerat-dev corerat-tools \
    libcommrat-dev \
    sshd-regen-keys expand-on-first-boot"

# TODO: uncomment when the ratgui recipe exists:
# IMAGE_INSTALL:append = " ratgui"

IMAGE_INSTALL:append:xenomai4 = " libevl-test"

ROOTFS_FEATURES:remove = "generate-sbom"

require recipes-core/images/ratos-container-qemu-setup.inc
