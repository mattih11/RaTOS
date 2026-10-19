#
# RaTOS Real-Time OS - shared EFI Boot Guard A/B integration
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

DEPENDS += " \
    swupdate-handler-roundrobin \
    swupdate-config \
    immutable-rootfs \
    journald-config \
    move-homedir-var \
    ratos-swupdate-config \
    "

include recipes-core/images/swupdate.inc

WKS_FILE = "odroid-h4-efibootguard.wks.in"

IMAGE_INSTALL:remove = "expand-on-first-boot"
IMAGE_INSTALL += "ratos-swupdate-config"
