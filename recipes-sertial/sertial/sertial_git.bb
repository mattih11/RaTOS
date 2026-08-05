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
SRCREV = "cd77ffebc4eec9e0db5ff591b1ec71b3e1119d1c"
PV = "2.1.0+git${SRCPV}"
S = "${WORKDIR}/git"

inherit dpkg

# Declare Debian binary package names as BitBake providers so
# SDK_INSTALL += "libsertial-dev" resolves correctly.
DEPENDS = "reflect-cpp"

PROVIDES += "libsertial-dev sertial-tools"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
