#
# RaTOS Real-Time OS - MusicRaT image
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

DESCRIPTION = "RaTOS MusicRaT image - EVL, CommRaT, and headless MusicRaT"
HOSTNAME = "ratos-musicrat"

DEPENDS = "linux-xenomai-4 libevl efibootguard \
           reflect-cpp sertial corerat commrat musicrat \
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
    musicrat libmusicrat-dev musicrat-examples \
    sshd-regen-keys expand-on-first-boot"

IMAGE_INSTALL:append:xenomai4 = " libevl-test"

ROOTFS_FEATURES:remove = "generate-sbom"

# MusicRaT links optional codecs from Debian. Refresh all configured package
# sources so the image resolves the same library revisions as its sbuild chroot.
rootfs_install_pkgs_update() {
    sudo -E chroot '${ROOTFSDIR}' /usr/bin/apt-get update \
        -o APT::Get::List-Cleanup="0"
}

require recipes-core/images/ratos-container-qemu-setup.inc
