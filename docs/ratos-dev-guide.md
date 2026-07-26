# RaTOS Developer Guide — QEMU + Cross-Compilation SDK

This guide covers the recommended workflow for developing **CoreRaT** and
**CommRaT** against RaTOS: **compile on the host** using the cross-compilation
SDK, and **run/test inside QEMU**.

The key principle is to always boot the image that contains your component's
dependencies but **not** the component itself — so your local build is the only
version present in the VM.

---

## Choosing the right image

| You are developing | Boot this image | Layer | Pre-installed | Not installed |
|---|---|---|---|---|
| **SeRTial** | `ratos-evl-image` | 0 | EVL kernel, libevl, reflect-cpp | SeRTial, CoreRaT, CommRaT |
| **CoreRaT** | `ratos-evl-image` | 0 | EVL kernel, libevl, reflect-cpp | CoreRaT, CommRaT |
| **CommRaT** | `ratos-sertial-image` | 1 | Layer 0 + SeRTial + CoreRaT | CommRaT |
| Applications on CommRaT | `ratos-commrat-image` | 2 | Layer 1 + CommRaT | your app |

Do **not** use `ratos-dev-image` for development — it has the full released
stack pre-installed and will shadow your local build.

---

## Quick start

Both workflows are identical in structure; only image names and deploy paths
differ.  Read the table above, pick your row, then follow the steps below.

### Step 1 — Get the image

#### Option A — Local build

```sh
# SeRTial / CoreRaT developer
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/evl.yaml

# CommRaT developer
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/sertial.yaml
```

Output lands in `build/tmp/deploy/images/container-amd64/`.

#### Option B — CI artifact (no local build)

Download from the latest `ratos-evl-artifacts` workflow artifact on the
[RaTOS `main` branch](https://github.com/mattih11/RaTOS/actions), or from any
[GitHub Release](https://github.com/mattih11/RaTOS/releases):

| Component | Files to download |
|---|---|
| SeRTial / CoreRaT | `ratos-evl-image-container-amd64-vmlinuz`, `ratos-evl-image-container-amd64-initrd.img`, `ratos-evl-image-container-amd64.ext4.gz` |
| CommRaT | `ratos-sertial-image-container-amd64-vmlinuz`, `ratos-sertial-image-container-amd64-initrd.img`, `ratos-sertial-image-container-amd64.ext4.gz` |

```sh
# Decompress whichever ext4 you downloaded
gunzip -k ratos-<image>-container-amd64.ext4.gz
```

---

### Step 2 — Boot QEMU

Replace `<image>` with `ratos-evl-image` (SeRTial/CoreRaT) or `ratos-sertial-image`
(CommRaT).

**From a local build:**
```sh
DEPLOY=build/tmp/deploy/images/container-amd64
IMAGE=<image>   # e.g. ratos-evl-image  or  ratos-sertial-image
ACCEL=tcg; [ -w /dev/kvm ] && ACCEL=kvm

qemu-system-x86_64 \
  -cpu qemu64 -smp 4 -m 2G \
  -machine q35,accel=${ACCEL} \
  -kernel  ${DEPLOY}/${IMAGE}-ratos-container-amd64-vmlinuz \
  -initrd  ${DEPLOY}/${IMAGE}-ratos-container-amd64-initrd.img \
  -drive   file=${DEPLOY}/${IMAGE}-ratos-container-amd64.ext4,discard=unmap,if=none,id=disk,format=raw \
  -device  ide-hd,drive=disk \
  -append  "root=/dev/sda rw rootwait console=ttyS0" \
  -serial  mon:stdio \
  -netdev  user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
  -device  virtio-net-pci,netdev=net \
  -nographic
```

**From CI artifacts** (after `gunzip`):
```sh
IMAGE=<image>   # e.g. ratos-evl-image  or  ratos-sertial-image
ACCEL=tcg; [ -w /dev/kvm ] && ACCEL=kvm

qemu-system-x86_64 \
  -cpu qemu64 -smp 4 -m 2G \
  -machine q35,accel=${ACCEL} \
  -kernel  ${IMAGE}-container-amd64-vmlinuz \
  -initrd  ${IMAGE}-container-amd64-initrd.img \
  -drive   file=${IMAGE}-container-amd64.ext4,discard=unmap,if=none,id=disk,format=raw \
  -device  ide-hd,drive=disk \
  -append  "root=/dev/sda rw rootwait console=ttyS0" \
  -serial  mon:stdio \
  -netdev  user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
  -device  virtio-net-pci,netdev=net \
  -nographic
```

QEMU starts with 4 vCPUs, 2 GB RAM, KVM if available.
Login: `root` / `root` — SSH: `ssh root@localhost -p 22222` — Quit: `Ctrl-A X`.

---

### Step 3 — Build the cross-compilation SDK

The SDK is always built from `ratos-dev-image` (contains the full stack of
headers including libevl, libcorerat-dev, libsertial-dev, libreflect-cpp-dev,
and libcommrat-dev) and works for both CoreRaT and CommRaT development.

**From a local build (run once; reuses sstate):**
```sh
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml \
  -- -c populate_sdk ratos-dev-image
```

**From a GitHub Release:**
```sh
# Download ratos-dev-sdk-container-amd64.tar.xz from the release page
```

**Install (either source):**
```sh
SDK_TAR=$(find build/tmp/deploy/sdkchroot -name '*.tar.xz' 2>/dev/null | head -1)
# or: SDK_TAR=ratos-dev-sdk-container-amd64.tar.xz

sudo mkdir -p /opt/ratos-sdk
sudo tar -xf "${SDK_TAR}" --strip-components=1 -C /opt/ratos-sdk
sudo /opt/ratos-sdk/relocate-sdk.sh
export PATH=/opt/ratos-sdk/usr/bin:$PATH
```

The SDK ships on **GitHub Release tags only** (`v*`). It is not included in
the `ratos-evl-artifacts` workflow artifact from non-tagged `main` builds.

---

### Step 4 — Cross-compile

```sh
# CoreRaT
cd /path/to/CoreRaT
cmake -B build-ratos \
  -DCMAKE_C_COMPILER=x86_64-linux-gnu-gcc \
  -DCMAKE_CXX_COMPILER=x86_64-linux-gnu-g++ \
  -DCMAKE_SYSROOT=/opt/ratos-sdk
cmake --build build-ratos

# CommRaT (same pattern)
cd /path/to/CommRaT
cmake -B build-ratos \
  -DCMAKE_C_COMPILER=x86_64-linux-gnu-gcc \
  -DCMAKE_CXX_COMPILER=x86_64-linux-gnu-g++ \
  -DCMAKE_SYSROOT=/opt/ratos-sdk
cmake --build build-ratos
```

Or use the `evl` CMake preset if the project defines one (sets sysroot and
EVL platform automatically).

---

### Step 5 — Deploy and test in QEMU

#### CoreRaT

```sh
# Sync the CoreRaT build into the running VM
rsync -avz -e 'ssh -p 22222' build-ratos/ root@localhost:/opt/corerat/

# Inside QEMU — start the router, run tests
ssh root@localhost -p 22222
  /opt/corerat/corerat-router-tcp &          # TCP IPC backend
  /opt/corerat/tests/corerat_test
```

#### CommRaT

CoreRaT and SeRTial must already be running in the VM (they are pre-installed in
`ratos-sertial-image` as Debian packages):

```sh
# Ensure the CoreRaT router is running (it may already be via systemd)
ssh root@localhost -p 22222 "systemctl start corerat-router-tcp || corerat-router-tcp &"

# Sync and test CommRaT
rsync -avz -e 'ssh -p 22222' build-ratos/ root@localhost:/opt/commrat/
ssh root@localhost -p 22222 "/opt/commrat/tests/commrat_test"
```

---

## Iterative development loop

```
edit → cmake --build build-ratos → rsync to VM → run/test → repeat
```

With KVM enabled this loop is fast:
- Cross-compilation on host: seconds
- `rsync` incremental sync: sub-second
- No rebuilding the rootfs image between iterations

---

## QEMU reference

| Goal | Command |
|---|---|
| Quit QEMU | `Ctrl-A X` |
| SSH into VM | `ssh root@localhost -p 22222` |
| Copy a single file | `scp -P 22222 mybinary root@localhost:/usr/local/bin/` |
| Rsync a build dir | `rsync -avz -e 'ssh -p 22222' build-ratos/ root@localhost:/opt/myapp/` |
| Check EVL is running | `evl ps` or `cat /proc/evl/version` |
| Check KVM available | `ls -la /dev/kvm` |
| CoreRaT hostname | `ratos-evl` (layer 1 image) |
| CommRaT hostname | `ratos-corerat` (layer 2 image) |

---

## SDK contents

The SDK is built from `ratos-dev-image` and contains the complete RaTOS
library stack for cross-compilation:

| Package | Provides | Used by |
|---|---|---|
| `linux-headers-xenomai-4` | EVL kernel headers (`<evl/*.h>`) | CoreRaT, CommRaT |
| `libevl` | EVL user-space lib headers + `.so`/`.a` | CoreRaT |
| `libcorerat-dev` | CoreRaT headers + CMake config (`CoreRaT::corerat`) | CommRaT |
| `libsertial-dev` | SeRTial headers + CMake config (`SeRTial::sertial`) | CoreRaT, CommRaT |
| `libreflect-cpp-dev` | reflect-cpp headers (SeRTial dependency) | CoreRaT, CommRaT |
| `libcommrat-dev` | CommRaT headers (for apps building on top of CommRaT) | Applications |

> The EVL network header changed in libevl r56: use `<evl/net/net.h>`, not
> `<evl/net.h>`.

The toolchain targets the same Debian Trixie amd64 sysroot as the QEMU VM,
so binaries built with the SDK run unmodified inside the VM.

---

## What changed recently

| Commit | Change |
|---|---|
| `a966bcd` | Layered images introduced (`ratos-evl-image`, `ratos-sertial-image`, `ratos-commrat-image`). This guide replaces `commrat-dev-guide.md`. |
| `a198ab5` | QEMU boot switched to raw `ext4` + `-device ide-hd`. |
| `574f4f9` | SDK build step added to CI; SDK published on release tags. |
| (recent) | `PubkeyAuthentication yes` enabled in dev images; root key-based SSH now works without password fallback. |
