#
# RaTOS Real-Time OS
# Recipe: SeRTial - zero-allocation C++20 binary serialization library
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "SeRTial: zero-allocation C++20 binary serialization using compile-time reflection"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/SeRTial.git;protocol=https;branch=main \
           file://debian/"
SRCREV = "${AUTOREV}"
PV = "2.0.0+git${SRCPV}"
S = "${WORKDIR}/git"

inherit dpkg

DEPENDS = "reflect-cpp"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
