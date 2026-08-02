#
# RaTOS Real-Time OS
# Recipe: CommRaT - modern C++20 real-time communication framework
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "CommRaT: C++20 real-time communication framework over RACK/TiMS with SeRTial serialization"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/CommRaT.git;protocol=https;branch=feature/mailbox-cleanup \
           file://debian/"
SRCREV = "075d5dc4b00c9aefd5dcb8204faadc461a4d7d19"
PV = "0.0+git${SRCPV}"
S = "${WORKDIR}/git"

DEPENDS = "sertial reflect-cpp corerat libevl"

inherit dpkg

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
