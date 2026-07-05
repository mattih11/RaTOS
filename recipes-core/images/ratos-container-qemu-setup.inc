#
# RaTOS Real-Time OS — QEMU / container-amd64 runtime setup
#
# Shared include for layered development images built for the
# container-amd64 machine.  Provides:
#   - hostname injection (reads HOSTNAME from the including recipe)
#   - systemd-networkd DHCP on all ethernet interfaces (en*)
#   - sshd_config that permits root login with password or pubkey
#     (intentional: images are for local QEMU development only)
#
# Usage in an image recipe:
#   HOSTNAME = "ratos-myimage"
#   require recipes-core/images/ratos-container-qemu-setup.inc
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

# Write /etc/hostname to suppress systemd-firstboot prompt during image build.
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

# Permit root SSH login and password/pubkey auth for the developer VM images.
# This is intentional: images using this include are for local QEMU development only.
# Note: root password ("root") is set by xenomai-demo.conf via ISAR's USERS mechanism.
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
