ISAR_RELEASE_CMD = "git -C ${LAYERDIR_ratos} describe --tags \
    --dirty --always --match 'v[0-9].[0-9]*'"

HOSTNAME = "ratos-installer"

ROOTFS_FEATURES:remove = "generate-sbom"

set_hostname() {
    echo "${HOSTNAME}" | sudo tee "${ROOTFSDIR}/etc/hostname" > /dev/null
}

ROOTFS_POSTPROCESS_COMMAND =+ "set_hostname"

rootfs_install_pkgs_update() {
    sudo -E chroot '${ROOTFSDIR}' /usr/bin/apt-get update \
        -o APT::Get::List-Cleanup="0"
}