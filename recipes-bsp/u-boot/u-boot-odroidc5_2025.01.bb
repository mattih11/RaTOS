#
# RaTOS Real-Time OS
#
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Odroid C5 (Amlogic S905X5M) U-Boot — pre-built binaries from Hardkernel.
#
# The Amlogic boot chain (BL2 + BL30 + BL31 + U-Boot) requires proprietary
# Amlogic firmware blobs that cannot be built from mainline sources.
# Hardkernel publishes a combined FIP binary (u-boot.bin) via their Ubuntu
# APT repository.  This recipe downloads the .deb directly and re-packages
# the firmware binary as an ISAR-native package so wic can reference it at
#   /usr/lib/u-boot/odroidc5/u-boot.bin
#
# Write offset: sector 1 (512 bytes) — as per Hardkernel sd_fusing.sh
#   dd if=u-boot.bin of=<dev> bs=512 seek=1
#

DESCRIPTION = "Odroid C5 U-Boot (Amlogic S905X5M, Hardkernel BSP)"
MAINTAINER = "Matthias Haase <mattihaase@proton.me>"
LICENSE = "GPL-2.0-or-later"

COMPATIBLE_MACHINE = "^(odroidc5)$"

# Hardkernel ppa.linuxfactory.or.kr – noble/main arm64
HARDKERNEL_PV = "2025.01+202605281243~noble"
SRC_URI = "https://ppa.linuxfactory.or.kr/pool/main/u/u-boot-odroidc5/u-boot-odroidc5_${HARDKERNEL_PV}_arm64.deb;downloadfilename=u-boot-odroidc5.deb"
SRC_URI[sha256sum] = "8a7f51e157593f027bdab87bf6cc9a8eaa4c26ae3bf6dc139f07eae13a873148"

# Architecture: the u-boot binary is arm64-specific but the package is
# declared 'all' for ISAR so the build host can install it as an imager dep.
DPKG_ARCH = "all"

inherit dpkg-raw

do_install[cleandirs] = "${D}/usr/lib/u-boot/odroidc5/"
do_install() {
    # Extract the Hardkernel .deb (ar + tar, no dpkg-deb needed on host)
    PKGDIR="${WORKDIR}/pkg-extract"
    mkdir -p "${PKGDIR}"
    cd "${WORKDIR}"
    ar x u-boot-odroidc5.deb
    tar xf data.tar.* -C "${PKGDIR}/"

    install -m 0644 \
        "${PKGDIR}/usr/lib/u-boot/odroidc5/u-boot.bin" \
        "${D}/usr/lib/u-boot/odroidc5/u-boot.bin"
}
