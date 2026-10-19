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

require recipes-core/images/ratos-swupdate.inc

DESCRIPTION = "RaTOS real-time image with SWUpdate A/B OTA support"
