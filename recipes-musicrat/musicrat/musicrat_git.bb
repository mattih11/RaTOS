#
# RaTOS Real-Time OS
# Recipe: MusicRaT - modular real-time audio framework
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DESCRIPTION = "MusicRaT: modular C++20 real-time audio framework"
MAINTAINER = "Matthias Haase"
LICENSE = "GPL-2.0-or-later"

SRC_URI = "git://github.com/mattih11/MusicRaT.git;protocol=https;branch=main;name=musicrat;destsuffix=git \
           git://github.com/lvgl/lvgl.git;protocol=https;branch=release/v9.6;name=lvgl;destsuffix=lvgl \
           file://debian/"
SRCREV_musicrat = "f2d4688d2378bd6178c8a5ede476f6d3e59352e6"
SRCREV_lvgl = "1093217a21639868a0014193c4d40edfcadee8ad"
SRCREV_FORMAT = "musicrat_lvgl"
PV = "1.0.1+git1"
S = "${WORKDIR}/git"

DEPENDS = "commrat corerat sertial reflect-cpp libevl"

inherit dpkg

PROVIDES += "musicrat libmusicrat-dev musicrat-examples"

do_prepare_build() {
    cp -Trl -- "${WORKDIR}/debian" "${S}/debian"
    mkdir -p "${S}/third_party"
    cp -a -- "${WORKDIR}/lvgl" "${S}/third_party/lvgl"
    rm -rf "${S}/third_party/lvgl/.git"
    chmod +x "${S}/debian/rules"
}
