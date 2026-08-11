#
# RaTOS Real-Time OS
# Recipe: CoreRaT - platform abstraction, wire types, EVL IPC backend
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "CoreRaT: C++20 real-time platform abstraction and IPC foundation library"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/CoreRaT.git;protocol=https;branch=main \
           file://debian/"
SRCREV = "c6e3f8ceb4be3e1b05af4e9a91b676edd9be9694"
PV = "0.0+git${SRCPV}"
S = "${WORKDIR}/git"

DEPENDS = "sertial reflect-cpp libevl"

inherit dpkg

# Declare Debian binary package names as BitBake providers so
# SDK_INSTALL += "libcorerat-dev" resolves correctly.
PROVIDES += "libcorerat-dev corerat-tools"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
