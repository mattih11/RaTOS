#
# RaTOS Real-Time OS
# Drop a backport patch from libevl_56 that is already included in
# the r56 release tarball (causes a "can be reverse-applied" quilt failure).
#
# SPDX-License-Identifier: GPL-3.0-or-later
#

SRC_URI:remove = "file://0001-utils-net-Add-missing-initialization-of-query_type.patch"
