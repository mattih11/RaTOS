#
# RaTOS Real-Time OS
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS real-time image (Xenomai/EVL, Odroid H4)"

# Local dependencies for our provided packages
DEPENDS = "rack reflect-cpp sertial commrat"

# EFI Boot Guard must be built so the wic imager can install it into the build schroot.
# (Does NOT install efibootguard-tools into the target rootfs — only ensures build+deploy.)
DEPENDS += "efibootguard"

# Pull in the full Xenomai 4 / EVL test suite from upstream
IMAGE_INSTALL:append:xenomai4 = " libevl-test"

# Base system utilities (mirrors upstream demo-image)
IMAGE_PREINSTALL += " \
    bash-completion less vim nano man \
    ifupdown isc-dhcp-client net-tools iputils-ping ssh \
    iw wireless-tools wpasupplicant systemd-timesyncd dbus \
    lsb-release auditd"

IMAGE_INSTALL += "customizations sshd-regen-keys expand-on-first-boot"

# RaTOS dependencies: built by local recipes and installed into the image.
# Package names come from each recipe's debian/control, not the recipe name.
IMAGE_INSTALL += "rack rack-dev libreflect-cpp-dev libsertial-dev sertial-tools libcommrat-dev"

ROOTFS_FEATURES:remove = "generate-sbom"
