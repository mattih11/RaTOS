#
# RaTOS Real-Time OS — SWUpdate A/B image
#
# Extends ratos-image with SWUpdate support (EFI Boot Guard A/B).
# Produces both a .wic (initial flash) and a .swu artifact (OTA update).
#
# Build:
#   kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/opt/swupdate.yaml
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

COMPATIBLE_MACHINE = "odroid-h4"

require recipes-core/images/ratos-image.bb

# Locally-built packages added by swupdate.inc — must be declared so bitbake
# builds and deploys them to isar-apt before do_rootfs_install runs.
DEPENDS += " \
    swupdate-handler-roundrobin \
    swupdate-config \
    immutable-rootfs \
    journald-config \
    move-homedir-var \
    "

# Pull in SWUpdate integration from isar-cip-core:
#  - inherits read-only-rootfs (squashfs default)
#  - adds IMAGE_FSTYPES += " swu"
#  - installs swupdate, swupdate-handler-roundrobin, swupdate-config, efibootguard-tools
include recipes-core/images/swupdate.inc

DESCRIPTION = "RaTOS real-time image with SWUpdate A/B OTA support"

# A/B partitions have fixed sizes — partition expansion on first boot is not needed
IMAGE_INSTALL:remove = "expand-on-first-boot"
