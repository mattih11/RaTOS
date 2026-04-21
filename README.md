# RaTOS — Real-Time OS for Odroid H4

RaTOS is an ISAR-based Debian Trixie image for the **Odroid H4** (Intel Alder Lake-N, amd64),
running a Xenomai 4 / EVL real-time kernel. It uses **EFI Boot Guard** (from
[isar-cip-core](https://gitlab.com/cip-project/cip-core/isar-cip-core)) as bootloader and
supports A/B rootfs OTA updates via **SWUpdate**.

A developer container image is also produced (docker-archive) with all RaTOS libraries and
the EVL toolchain pre-installed.

## Prerequisites

- [kas-container](https://kas.readthedocs.io/) (version 5.1+)
- `podman` or `docker`
- `python3-kconfiglib` or `kconfig-frontends` (for the interactive menu)

## Quick Start

```bash
# Interactive build menu — select target and image type, then build
kas-container menu
```

## Image Types

| Image type | kas command | Output |
|---|---|---|
| **Production** (A/B SWUpdate OTA) | `kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/opt/swupdate.yaml` | `.wic` + `.swu` |
| **Development** (single-root) | `kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml` | `.wic` |
| **Dev container** | `kas-container --isar build kas.yaml:kas/board/container-amd64.yaml` | `docker-archive.gz` |

## Output Files

| Target | Path |
|---|---|
| Odroid H4 `.wic` | `build/tmp/deploy/images/odroid-h4/ratos-image*-odroid-h4.wic` |
| Odroid H4 `.swu` | `build/tmp/deploy/images/odroid-h4/ratos-image-swupdate*-odroid-h4.swu` |
| Dev container | `build/tmp/deploy/images/container-amd64/ratos-dev-image*-container-amd64.docker-archive.gz` |

## Flash to USB / SD Card

```bash
dd if=build/tmp/deploy/images/odroid-h4/ratos-image-ratos-odroid-h4.wic \
   of=/dev/sdX bs=4M status=progress
```

Or with bmap-tools (faster, only writes used blocks):

```bash
bmaptool copy build/tmp/deploy/images/odroid-h4/ratos-image-ratos-odroid-h4.wic /dev/sdX
```

## OTA Update

Copy the `.swu` to the running device and apply:

```bash
scp ratos-image-swupdate*.swu root@<device>:
ssh root@<device> swupdate -i ratos-image-swupdate*.swu
# Device reboots into the new slot; confirm update:
ssh root@<device> bg_setenv -c
```

## Load the Developer Container

```bash
podman load < build/tmp/deploy/images/container-amd64/ratos-dev-image-ratos-container-amd64.docker-archive.gz
podman tag $(podman images -q | head -1) ratos-dev-image:latest
podman run -it --rm -v $PWD:/workspace ratos-dev-image:latest
```

## Project Structure

```
kas.yaml                       # top-level kas config (distro, repos)
Kconfig                        # interactive build menu
kas/board/                     # per-target kas overlays
kas/opt/                       # optional feature overlays (swupdate, …)
conf/machine/                  # machine configs (DISTRO_ARCH, EBG, WKS_FILE)
conf/distro/                   # distro config (extends xenomai-demo)
recipes-core/images/           # image recipes
  ratos-image.bb               # single-root development image
  ratos-image-swupdate.bb      # A/B SWUpdate production image (extends above)
  ratos-dev-image.bb           # developer container image
recipes-rack/                  # RACK middleware
recipes-sertial/               # SeRTial serialization library
recipes-reflect-cpp/           # reflect-cpp (SeRTial dependency)
recipes-commrat/               # CommRaT application
wic/                           # disk layouts
  odroid-h4.wks                # single-root (EFI Boot Guard, single slot)
  odroid-h4-efibootguard.wks.in # A/B SWUpdate (templated, two slots)
```

## Key Dependencies (submodules / kas-pinned repos)

| Repo | Role |
|---|---|
| [isar](https://github.com/ilbers/isar) | ISAR build system |
| [xenomai-images](https://gitlab.com/Xenomai/xenomai-images) | Xenomai 4/EVL kernel + libevl recipes |
| [isar-cip-core](https://gitlab.com/cip-project/cip-core/isar-cip-core) | EFI Boot Guard, SWUpdate, A/B wic plugins |

## License

GPL-3.0-or-later
