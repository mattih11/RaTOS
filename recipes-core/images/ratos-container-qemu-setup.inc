#
# RaTOS Real-Time OS - wired DHCP and SSH development access
#
# Provides:
#   - hostname injection (reads HOSTNAME from the including recipe)
#   - systemd-networkd IPv4 DHCP on every Ethernet interface
#   - sshd with root password and public-key authentication
#
# The default root password is supplied by the upstream ISAR image
# configuration. Change it before attaching a development image to an
# untrusted network.
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

# Write /etc/hostname to suppress systemd-firstboot during image build.
set_hostname() {
    echo "${HOSTNAME}" | sudo tee "${ROOTFSDIR}/etc/hostname" > /dev/null
}

configure_wired_networking() {
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/network"
    printf '[Match]\nType=ether\n\n[Network]\nDHCP=ipv4\nLinkLocalAddressing=yes\n' | \
        sudo tee "${ROOTFSDIR}/etc/systemd/network/10-wired-dhcp.network" > /dev/null
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/system/multi-user.target.wants"
    sudo ln -sf /usr/lib/systemd/system/systemd-networkd.service \
        "${ROOTFSDIR}/etc/systemd/system/multi-user.target.wants/systemd-networkd.service"
    sudo ln -sf /usr/lib/systemd/system/systemd-resolved.service \
        "${ROOTFSDIR}/etc/systemd/system/multi-user.target.wants/systemd-resolved.service"
    sudo ln -sf /run/systemd/resolve/stub-resolv.conf \
        "${ROOTFSDIR}/etc/resolv.conf"
}

configure_sshd() {
    sudo mkdir -p "${ROOTFSDIR}/etc/ssh/sshd_config.d"
    printf 'PermitRootLogin yes\nPasswordAuthentication yes\nPubkeyAuthentication yes\nListenAddress 0.0.0.0\nUseDNS no\n' | \
        sudo tee "${ROOTFSDIR}/etc/ssh/sshd_config.d/99-ratos-dev.conf" > /dev/null
    # Run one IPv4-capable sshd service instead of Debian socket activation.
    sudo mkdir -p "${ROOTFSDIR}/etc/systemd/system"
    sudo ln -sf /dev/null "${ROOTFSDIR}/etc/systemd/system/ssh.socket"
}

ROOTFS_POSTPROCESS_COMMAND =+ "set_hostname configure_wired_networking configure_sshd"
