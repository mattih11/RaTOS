# RaTOS — Real-Time OS

RaTOS is an ISAR-based Debian Trixie image running a Xenomai 4 / EVL real-time kernel.
It targets **amd64 / x86-64** hardware and uses **EFI Boot Guard** (from
[isar-cip-core](https://gitlab.com/cip-project/cip-core/isar-cip-core)) as bootloader.
A/B rootfs OTA updates are supported via **SWUpdate**.

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
| **Target — production** (A/B SWUpdate OTA) | `kas-container --isar build kas.yaml:kas/board/<board>.yaml:kas/opt/swupdate.yaml` | `.wic` + `.swu` |
| **Target — development** (single-root) | `kas-container --isar build kas.yaml:kas/board/<board>.yaml` | `.wic` |
| **Dev container** (amd64 host) | `kas-container --isar build kas.yaml:kas/board/container-amd64.yaml` | `docker-archive.gz` |

Replace `<board>` with the board overlay from `kas/board/` that matches your hardware.
The current in-tree board is `odroid-h4` (Intel Alder Lake-N N97, amd64, standard UEFI).

## Output Files

| Target | Path |
|---|---|
| Board `.wic` | `build/tmp/deploy/images/<board>/ratos-image*-<board>.wic` |
| Board `.swu` | `build/tmp/deploy/images/<board>/ratos-image-swupdate*-<board>.swu` |
| Dev container | `build/tmp/deploy/images/container-amd64/ratos-dev-image*-container-amd64.docker-archive.gz` |

## Flash to USB / SD Card

```bash
dd if=build/tmp/deploy/images/<board>/ratos-image-ratos-<board>.wic \
   of=/dev/sdX bs=4M status=progress
```

Or with bmap-tools (faster, only writes used blocks):

```bash
bmaptool copy build/tmp/deploy/images/<board>/ratos-image-ratos-<board>.wic /dev/sdX
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
kas/board/                     # per-board kas overlays (one file per board)
kas/opt/                       # optional feature overlays (swupdate, ...)
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
wic/                           # disk layouts (one set per board)
```

## Key Dependencies (submodules / kas-pinned repos)

| Repo | Role |
|---|---|
| [isar](https://github.com/ilbers/isar) | ISAR build system |
| [xenomai-images](https://gitlab.com/Xenomai/xenomai-images) | Xenomai 4/EVL kernel + libevl recipes |
| [isar-cip-core](https://gitlab.com/cip-project/cip-core/isar-cip-core) | EFI Boot Guard, SWUpdate, A/B wic plugins |

## CI / CD

The repository ships a GitHub Actions workflow at
[.github/workflows/build-and-publish.yml](.github/workflows/build-and-publish.yml)
that runs on every push to `main`, on version tags (`v*`), and on manual trigger
(`workflow_dispatch`).

### What the pipeline does

| Step | Details |
|---|---|
| **Build** | Runs `kas-container --isar build kas.yaml:kas/board/container-amd64.yaml` on an `ubuntu-latest` runner (privileged Docker is available on GitHub-hosted VMs). Timeout: 120 min. |
| **GHCR push** | Loads the `.docker-archive.gz` and pushes `ghcr.io/<owner>/ratos-dev-image:latest` and `ghcr.io/<owner>/ratos-dev-image:<git-sha>`. |
| **GitHub Release** | On version tags only — creates a release and attaches the three artifacts below. |
| **Workflow artifact** | On non-tag events — uploads the three artifacts as `ratos-evl-artifacts` (7-day retention) for downstream CI to consume via `dawidd6/action-download-artifact`. |

### Published artifacts

| File | Description |
|---|---|
| `vmlinuz` | EVL kernel image |
| `initrd.img` | initrd for QEMU boot |
| `ratos-dev-image.docker-archive.gz` | Docker rootfs archive |

### Triggering a release

Tag a commit with a `v`-prefixed version; the pipeline creates the GitHub
Release and attaches all three artifacts automatically:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Using the Docker image from GHCR

```bash
docker pull ghcr.io/<owner>/ratos-dev-image:latest
docker run -it --rm -v $PWD:/workspace ghcr.io/<owner>/ratos-dev-image:latest
```

### Consuming artifacts from a non-release build

Downstream CI can download `ratos-evl-artifacts` from the last `main` build
using [dawidd6/action-download-artifact](https://github.com/dawidd6/action-download-artifact):

```yaml
- uses: dawidd6/action-download-artifact@v6
  with:
    repo: <owner>/RaTOS
    workflow: build-and-publish.yml
    branch: main
    name: ratos-evl-artifacts
```

## License

GPL-3.0-or-later
