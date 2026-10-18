#
# RaTOS Real-Time OS
# Recipe: CommRaT - modern C++20 real-time communication framework
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "CommRaT: C++20 real-time communication framework over RACK/TiMS with SeRTial serialization"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/CommRaT.git;protocol=https;branch=main \
           file://debian/"
SRCREV = "f2eaaf9001b402b068b5cd17ca5cd8b0169b3093"
PV = "0.0+git${SRCPV}"
S = "${WORKDIR}/git"

DEPENDS = "sertial reflect-cpp corerat libevl"

inherit dpkg

# Declare the Debian development package as a BitBake provider so
# SDK_INSTALL += "libcommrat-dev" resolves correctly.
PROVIDES += "libcommrat-dev"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
