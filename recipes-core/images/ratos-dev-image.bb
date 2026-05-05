#
# RaTOS Real-Time OS — Developer Container Image
#
# Produces a docker-archive.gz suitable for loading with:
#   podman load < ratos-dev-image-ratos-container-amd64.docker-archive.gz
#
# Build:
#   kas-container --isar build kas.yaml:kas/board/container-amd64.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit image

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags --dirty --always --match 'v[0-9].[0-9]*'"
DESCRIPTION = "RaTOS developer container image with EVL/Xenomai 4 toolchain"
HOSTNAME = "ratos-dev"

# Entire RaTOS stack built by local recipes
DEPENDS = "rack reflect-cpp sertial commrat"

# Ensure the EVL kernel and libevl are built and deployed to isar-apt before
# do_rootfs_install runs. image.bbclass adds linux-image-xenomai-4 (package name)
# to DEPENDS via IMAGE_INSTALL, but bitbake can't resolve that to a recipe because
# the recipe PN is linux-xenomai-4 and PROVIDES uses the :arch suffix form. Spelling
# out the recipe PNs here ensures the deptask fires on a cold cache (e.g. CI).
DEPENDS += "linux-xenomai-4 libevl"
DEPENDS += "sshd-regen-keys"

# Dev toolchain from Debian Trixie
IMAGE_PREINSTALL += " \
    build-essential cmake ninja-build pkg-config \
    g++ gdb valgrind \
    git ca-certificates curl \
    python3 python3-pip \
    clang-format clang-tidy \
    openssh-server \
    rsync \
    "

# RaTOS and EVL development libraries (headers + link stubs)
# libevl ships headers in the main package (no separate -dev split)
IMAGE_INSTALL += " \
    libevl \
    rack-dev \
    libreflect-cpp-dev \
    libsertial-dev \
    libcommrat-dev \
    sshd-regen-keys \
    "

ROOTFS_FEATURES:remove = "generate-sbom"

# Write /etc/hostname to suppress systemd-firstboot prompt during image build
set_hostname() {
    echo "${HOSTNAME}" | sudo tee "${ROOTFSDIR}/etc/hostname" > /dev/null
}

# Enable DHCP on all ethernet interfaces via systemd-networkd for QEMU SLIRP networking.
configure_networking() {
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/network"
    printf '[Match]\nName=en*\n\n[Network]\nDHCP=yes\n' | \
        sudo tee "${ROOTFSDIR}/etc/systemd/network/10-dhcp.network" > /dev/null
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/system/multi-user.target.wants"
    sudo ln -sf /usr/lib/systemd/system/systemd-networkd.service \
        "${ROOTFSDIR}/etc/systemd/system/multi-user.target.wants/systemd-networkd.service"
}

# Permit root SSH login and password auth for the developer VM image.
# This is intentional: the image is for local QEMU development only.
# Note: root password ("root") is set by xenomai-demo.conf via ISAR's USERS
# mechanism (USER_root[password]) — no chpasswd needed here.
configure_sshd() {
    sudo mkdir -p "${ROOTFSDIR}/etc/ssh/sshd_config.d"
    printf 'PermitRootLogin yes\nPasswordAuthentication yes\nPubkeyAuthentication yes\nListenAddress 0.0.0.0\nUseDNS no\n' | \
        sudo tee "${ROOTFSDIR}/etc/ssh/sshd_config.d/99-ratos-dev.conf" > /dev/null
    # Disable socket activation so sshd binds its own sockets and respects
    # ListenAddress. Without this, systemd passes an IPv6-only socket and
    # QEMU's IPv4 port-forward (hostfwd=tcp:127.0.0.1:22222-:22) cannot connect.
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/system"
    sudo ln -sf /dev/null "${ROOTFSDIR}/etc/systemd/system/ssh.socket"
}

ROOTFS_POSTPROCESS_COMMAND =+ "set_hostname configure_networking configure_sshd"
