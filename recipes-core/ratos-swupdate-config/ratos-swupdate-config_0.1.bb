#
# SPDX-License-Identifier: GPL-3.0-or-later
#

inherit dpkg-raw

DESCRIPTION = "RaTOS A/B runtime watchdog configuration"
MAINTAINER = "RaTOS developers"

SRC_URI = "file://99-ratos-watchdog.conf"

do_install() {
    install -d "${D}/etc/systemd/system.conf.d"
    install -m 0644 "${WORKDIR}/99-ratos-watchdog.conf" \
        "${D}/etc/systemd/system.conf.d/99-ratos-watchdog.conf"
}
