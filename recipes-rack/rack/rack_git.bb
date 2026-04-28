#
# RaTOS Real-Time OS
# Recipe: RACK - Robotics Application Construction Kit (TiMS IPC backend)
#
# SPDX-License-Identifier: GPL-3.0-or-later
#
# NOTE: RACK towards-cmake branch uses CMake. Adjust cmake flags below
# if the upstream CMakeLists option names change.
#

DESCRIPTION = "RACK: Robotics Application Construction Kit providing TiMS IPC messaging"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/RACK.git;protocol=https;branch=fixed-towards-cmake \
           file://debian/"
SRCREV = "${AUTOREV}"
PV = "0.7.0+git${SRCPV}"
S = "${WORKDIR}/git/rack"

inherit dpkg

# Declare Debian binary package names as BitBake providers so
# SDK_INSTALL += "rack-dev" resolves correctly.
PROVIDES += "rack-dev rack-tools"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    chmod +x "${S}/debian/rules"
}
