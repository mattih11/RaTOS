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

RaTOS provides four **layered development images** plus the full production and
developer-container images.  Each layer adds one component on top of the previous.
Use the smallest image that satisfies your development needs — it is faster to
build and test and avoids conflicts with pre-installed versions of the component
you are working on.

| Layer | Image recipe | KAS command | Typical use |
|---|---|---|---|
| 0 | `ratos-evl-image` | `kas.yaml:kas/board/<board>.yaml:kas/target/evl.yaml` | SeRTial development — bare EVL + reflect-cpp, no SeRTial pre-installed |
| 1 | `ratos-sertial-image` | `kas.yaml:kas/board/<board>.yaml:kas/target/sertial.yaml` | CommRaT development — SeRTial + CoreRaT pre-installed |
| 2 | `ratos-commrat-image` | `kas.yaml:kas/board/<board>.yaml:kas/target/commrat.yaml` | Application development — SeRTial + CoreRaT + CommRaT pre-installed |
| 3 | `ratos-ratgui-image` | `kas.yaml:kas/board/<board>.yaml:kas/target/ratgui.yaml` | RatGUI development — full RT stack pre-installed |
| 4 | `ratos-image` | `kas.yaml:kas/board/<board>.yaml` | Production / QA — full stack |
| — | `ratos-image-swupdate` | `kas.yaml:kas/board/<board>.yaml:kas/opt/swupdate.yaml` | Production with A/B OTA (extends layer 4) |
| — | `ratos-dev-image` | `kas.yaml:kas/board/container-amd64.yaml` | Dev container — full stack + build toolchain, SDK |

Replace `<board>` with the board overlay from `kas/board/` that matches your hardware.
The current in-tree boards are `odroid-h4` (Intel N-series, UEFI), `odroid-c5` (Amlogic S905X5M), and `odroid-m1s` (Rockchip RK3566).

See [docs/layered-development-guide.md](docs/layered-development-guide.md) for a detailed
explanation of the layering model and how to choose the right image.

## Output Files

All files land under `build/tmp/deploy/images/<machine>/`.  Replace `<image>` with
the image recipe name (e.g. `ratos-evl-image`, `ratos-sertial-image`, `ratos-dev-image`).

| Target | Path pattern |
|---|---|
| Board `.wic.gz` | `<image>-ratos-<board>.wic.gz` |
| Board `.swu` (SWUpdate) | `ratos-image-swupdate-ratos-<board>.swu` |
| container-amd64 `ext4` | `<image>-ratos-container-amd64.ext4` |
| container-amd64 `wic.gz` | `<image>-ratos-container-amd64.wic.gz` |
| container-amd64 kernel | `<image>-ratos-container-amd64-vmlinuz` |
| container-amd64 initrd | `<image>-ratos-container-amd64-initrd.img` |
| Dev container docker | `ratos-dev-image-ratos-amd64.docker-archive.gz` |

## Layered Development Images

RaTOS follows a layered model where each image adds exactly one component.
Build the smallest image that contains your runtime dependency — the one below it.

| Layer | Image | Contains | Use when developing |
|---|---|---|---|
| 0 | `ratos-evl-image` | EVL kernel + libevl + reflect-cpp | **SeRTial** |
| 1 | `ratos-sertial-image` | Layer 0 + SeRTial + CoreRaT | **CommRaT** |
| 2 | `ratos-commrat-image` | Layer 1 + CommRaT | Applications / **RatGUI** |
| 3 | `ratos-ratgui-image` | Layer 2 + RatGUI | Production UI |
| 4 | `ratos-image` | Full production stack | QA / release |

```sh
# Example: CommRaT developer boots layer 1 — SeRTial + CoreRaT pre-installed, no CommRaT
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/sertial.yaml
```

For the full explanation, QEMU boot instructions per layer, and guidance on
future repo separation, see [docs/layered-development-guide.md](docs/layered-development-guide.md).

---

## Boot the Developer Image in QEMU

The simplest way is the helper script (uses `ext4` + KVM if available):

```bash
./scripts/start-qemu.sh
```

Or manually, using the local build outputs:

```bash
DEPLOY=build/tmp/deploy/images/container-amd64
ACCEL=tcg; [ -w /dev/kvm ] && ACCEL=kvm
qemu-system-x86_64 \
  -cpu host -enable-kvm -smp 4 -m 2G \
  -machine q35,accel=${ACCEL} \
  -kernel  ${DEPLOY}/ratos-dev-image-ratos-container-amd64-vmlinuz \
  -initrd  ${DEPLOY}/ratos-dev-image-ratos-container-amd64-initrd.img \
  -drive   file=${DEPLOY}/ratos-dev-image-ratos-container-amd64.ext4,discard=unmap,if=none,id=disk,format=raw \
  -device  ide-hd,drive=disk \
  -append  "root=/dev/sda rw rootwait console=ttyS0" \
  -serial  mon:stdio \
  -netdev  user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
  -device  virtio-net-pci,netdev=net \
  -nographic
```

See [docs/ratos-dev-guide.md](docs/ratos-dev-guide.md) for the combined CoreRaT /
CommRaT developer workflow.  For the layered image model and how to choose the
right image, see [docs/layered-development-guide.md](docs/layered-development-guide.md).

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
kas/target/                    # image-selection overlays (evl / corerat / commrat)
kas/opt/                       # optional feature overlays (swupdate, ...)
conf/machine/                  # machine configs (DISTRO_ARCH, EBG, WKS_FILE)
conf/distro/                   # distro config (extends xenomai-demo)
recipes-core/images/
  ratos-container-qemu-setup.inc  # shared QEMU hostname / network / sshd setup
  ratos-evl-image.bb              # layer 0: EVL kernel + libevl + reflect-cpp
  ratos-sertial-image.bb          # layer 1: layer 0 + SeRTial + CoreRaT
  ratos-commrat-image.bb          # layer 2: layer 1 + CommRaT
  ratos-ratgui-image.bb           # layer 3: layer 2 + RatGUI
  ratos-image.bb                  # layer 4: full production image
  ratos-image-swupdate.bb         # layer 4 + A/B SWUpdate OTA
  ratos-dev-image.bb              # developer container (full stack + toolchain)
recipes-corerat/               # CoreRaT platform library recipe
recipes-commrat/               # CommRaT communication framework recipe
recipes-sertial/               # SeRTial serialization library
recipes-reflect-cpp/           # reflect-cpp (SeRTial dependency)
recipes-xenomai/               # libevl bbappend
recipes-bsp/                   # U-Boot recipe
wic/                           # disk layouts (one set per board)
docs/                          # developer guides
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
| **Build (matrix)** | One job per machine in parallel. `container-amd64` builds `ratos-dev-image` then all four layered images (EVL → SeRTial → CommRaT → RatGUI); `odroid-h4` builds `ratos-image` (`continue-on-error`). Timeout: 180 min. |
| **Layered builds** | After the main `container-amd64` build, the four layered images are built in the same job, reusing sstate — only rootfs assembly runs (fast). |
| **GHCR push** | Pushes SDK images for each layer: `ghcr.io/<owner>/ratos-evl-sdk`, `ratos-sertial-sdk`, `ratos-commrat-sdk`, and `ratos-dev-image`. Tagged with `:latest` and `:<git-sha>`. |
| **GitHub Release** | On version tags only — creates a release and attaches all artifacts from all successful matrix jobs. |
| **Workflow artifact** | On non-tag events — uploads container-amd64 artifacts as `ratos-evl-artifacts` (7-day retention) for downstream CI. |

### Published artifacts

**container-amd64** (always present on successful build):

| File | Layer | Description |
|---|---|---|
| `vmlinuz` | — | Alias → `ratos-dev-image` kernel (backward compat) |
| `initrd.img` | — | Alias → `ratos-dev-image` initrd (backward compat) |
| `ratos-dev-image-container-amd64-vmlinuz` | dev | Full dev-image EVL kernel |
| `ratos-evl-image-container-amd64-vmlinuz` | 0 | EVL-only image kernel |
| `ratos-sertial-image-container-amd64-vmlinuz` | 1 | SeRTial image kernel |
| `ratos-commrat-image-container-amd64-vmlinuz` | 2 | CommRaT image kernel |
| `ratos-ratgui-image-container-amd64-vmlinuz` | 3 | RatGUI image kernel |
| `ratos-dev-image-container-amd64.ext4.gz` | dev | Full dev-image ext4 rootfs |
| `ratos-evl-image-container-amd64.ext4.gz` | 0 | EVL-only ext4 rootfs |
| `ratos-sertial-image-container-amd64.ext4.gz` | 1 | SeRTial ext4 rootfs |
| `ratos-commrat-image-container-amd64.ext4.gz` | 2 | CommRaT ext4 rootfs |
| `ratos-ratgui-image-container-amd64.ext4.gz` | 3 | RatGUI ext4 rootfs |
| `ratos-dev-image.docker-archive.gz` | dev | Docker rootfs archive |
| `ratos-evl-image.docker-archive.gz` | 0 | EVL SDK Docker archive (→ `ratos-evl-sdk` on GHCR) |
| `ratos-sertial-image.docker-archive.gz` | 1 | SeRTial SDK Docker archive (→ `ratos-sertial-sdk` on GHCR) |
| `ratos-commrat-image.docker-archive.gz` | 2 | CommRaT SDK Docker archive (→ `ratos-commrat-sdk` on GHCR) |

(Each image also ships a matching `-initrd.img` and `.wic.gz`.)

**container-amd64** (version tags only):

| File | Description |
|---|---|
| `ratos-dev-sdk-container-amd64.*` | Cross-compilation SDK (`tar.xz` rootfs — extract and run `relocate-sdk.sh`) |

**odroid-h4** (`continue-on-error` — attached if the build succeeds):

| File | Description |
|---|---|
| `ratos-image-odroid-h4.wic.gz` | Full A/B EFI disk image (compressed) |

### Triggering a release

Tag a commit with a `v`-prefixed version; the pipeline creates the GitHub
Release and attaches all available matrix artifacts automatically:

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

Downstream CI can download `ratos-evl-artifacts` (container-amd64 outputs only)
from the last `main` build using [dawidd6/action-download-artifact](https://github.com/dawidd6/action-download-artifact):

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
