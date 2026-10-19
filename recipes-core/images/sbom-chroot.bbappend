# New initramfs dependencies must not use expired Debian bootstrap indexes.
rootfs_install_pkgs_update() {
    sudo -E chroot '${ROOTFSDIR}' /usr/bin/apt-get update \
        -o APT::Get::List-Cleanup="0"
}
