#
# RaTOS Real-Time OS
# Recipe: reflect-cpp - compile-time C++20 reflection library (header-only)
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "reflect-cpp: compile-time C++20 reflection for serialization and deserialization"
MAINTAINER = "Matthias Haase"
LICENSE = "MIT"

SRC_URI = "git://github.com/getml/reflect-cpp.git;protocol=https;branch=main \
           file://debian/"
SRCREV = "${AUTOREV}"
PV = "0.0+git${SRCPV}"
S = "${WORKDIR}/git"

inherit dpkg

# Declare the Debian binary package name as a BitBake provider so
# SDK_INSTALL += "libreflect-cpp-dev" resolves correctly.
PROVIDES += "libreflect-cpp-dev"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
