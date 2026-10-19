#
# RaTOS Real-Time OS - MusicRaT with EFI Boot Guard A/B updates
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

COMPATIBLE_MACHINE = "odroid-h4"

require recipes-core/images/ratos-musicrat-image.bb
require recipes-core/images/ratos-swupdate.inc

DESCRIPTION = "RaTOS MusicRaT image with SWUpdate A/B OTA support"
SWU_NAME = "RaTOS MusicRaT"
