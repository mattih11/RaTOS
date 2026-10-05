#
# RaTOS Real-Time OS - MusicRaT Zeroconf service advertisement
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "Zeroconf SSH service advertisement for MusicRaT devices"
MAINTAINER = "Matthias Haase <mattihaase@proton.me>"
LICENSE = "GPL-3.0-or-later"

SRC_URI = "file://musicrat.service"

DPKG_ARCH = "all"
DEBIAN_DEPENDS = "avahi-daemon"

inherit dpkg-raw

do_install() {
    install -D -m 0644 "${WORKDIR}/musicrat.service" \
        "${D}/etc/avahi/services/musicrat.service"
}