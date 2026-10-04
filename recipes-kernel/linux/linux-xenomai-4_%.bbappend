FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:odroid-h4 = " file://odroid-h4-boot.cfg"