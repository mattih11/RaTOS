#
# RaTOS Real-Time OS
#
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Odroid M1S (Rockchip RK3566) U-Boot — pre-built binaries from Hardkernel.
#
# Hardkernel publishes a combined bootloader package for the M1S via their
# Ubuntu APT repository.  This recipe downloads the .deb directly and
# re-packages the firmware binaries as an ISAR-native package so wic can
# reference them at:
#   /usr/lib/u-boot/odroidm1s/u-boot-spl.img  (idbloader / SPL)
#   /usr/lib/u-boot/odroidm1s/uboot.img        (U-Boot proper)
#
# Write offsets (from Hardkernel sd_fusing.sh):
#   dd if=uboot.img       of=<dev> bs=512 seek=2048  → 1 MiB
#   dd if=u-boot-spl.img  of=<dev> bs=512 seek=64    → 32 KiB
#

DESCRIPTION = "Odroid M1S U-Boot (Rockchip RK3566, Hardkernel BSP)"
MAINTAINER = "Matthias Haase <mattihaase@proton.me>"
LICENSE = "GPL-2.0-or-later"

COMPATIBLE_MACHINE = "^(odroidm1s)$"

# Hardkernel ppa.linuxfactory.or.kr – noble/main arm64
HARDKERNEL_PV = "2017.09+202604250217"
SRC_URI = "https://ppa.linuxfactory.or.kr/pool/main/u/u-boot-odroidm1s/u-boot-odroidm1s_${HARDKERNEL_PV}_arm64.deb;downloadfilename=u-boot-odroidm1s.deb"
SRC_URI[sha256sum] = "e8e6108818199203379872b5ea2633e3838d9cabafb3624813ecd32217b7a094"

# Architecture: arm64-specific binaries, declared 'all' so the build host
# can install the package as an imager dependency.
DPKG_ARCH = "all"

inherit dpkg-raw

do_install[cleandirs] = "${D}/usr/lib/u-boot/odroidm1s/"
do_install() {
    PKGDIR="${WORKDIR}/pkg-extract"
    mkdir -p "${PKGDIR}"
    cd "${WORKDIR}"
    ar x u-boot-odroidm1s.deb
    tar xf data.tar.* -C "${PKGDIR}/"

    install -m 0644 \
        "${PKGDIR}/usr/lib/u-boot/odroidm1s/u-boot-spl.img" \
        "${D}/usr/lib/u-boot/odroidm1s/u-boot-spl.img"
    install -m 0644 \
        "${PKGDIR}/usr/lib/u-boot/odroidm1s/uboot.img" \
        "${D}/usr/lib/u-boot/odroidm1s/uboot.img"
}
