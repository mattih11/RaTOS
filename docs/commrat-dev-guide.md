# CommRaT Developer Guide — QEMU + Cross-Compilation SDK

This guide covers the recommended workflow for CommRaT developers who want
to build for the RaTOS EVL target: **compile on the host** using the
cross-compilation SDK, and **run/test inside QEMU**.

## What changed recently

| Commit | Change |
|---|---|
| `a198ab5` | QEMU boot switched to raw `ext4` + `-device ide-hd` (aligns with upstream xenomai-images). No virtio modules needed. Root password is `root` (set by `xenomai-demo.conf`). `scripts/start-qemu.sh` updated accordingly. |
| `a198ab5` | `IMAGE_FSTYPES` for `container-amd64` is now `docker-archive.gz ext4 wic.gz`. The `ext4` file is used by `start-qemu.sh`; the `wic.gz` is the CI release artifact. |
| CI | SDK build step added for version-tag builds. The SDK installer (`ratos-dev-sdk-container-amd64.*`) is published as a release asset. Compressed ext4 (`ratos-dev-image-container-amd64.ext4.gz`) also published so you can boot from CI artifacts without rebuilding. |

---

## Quick start

### Option A — Local build (recommended for everyday dev)

**1. Build the image**

```sh
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml
```

Produces under `build/tmp/deploy/images/container-amd64/`:
- `ratos-dev-image-ratos-container-amd64.ext4` — rootfs for QEMU
- `ratos-dev-image-ratos-container-amd64-vmlinuz` — EVL kernel
- `ratos-dev-image-ratos-container-amd64-initrd.img` — initrd

**2. Build the cross-compilation SDK**

```sh
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml \
  -- -c populate_sdk ratos-dev-image
```

The SDK installer is written to `build/tmp/deploy/sdk/` (or
`build/tmp/deploy/images/container-amd64/`). Run it:

```sh
# Make executable and install (default prefix /opt/ratos-sdk)
chmod +x build/tmp/deploy/sdk/ratos*.sh
./build/tmp/deploy/sdk/ratos*.sh
```

**3. Source the SDK environment**

```sh
source /opt/ratos-sdk/environment-setup-*
```

This sets `CC`, `CXX`, `PKG_CONFIG_PATH`, `SYSROOT`, etc. for the
`x86_64 → amd64` cross-toolchain.

**4. Cross-compile CommRaT**

```sh
cd /path/to/commrat
cmake -B build-ratos -DCMAKE_TOOLCHAIN_FILE="${OECORE_NATIVE_SYSROOT}/usr/share/cmake/OEToolchainConfig.cmake"
cmake --build build-ratos
```

**5. Boot QEMU**

```sh
./scripts/start-qemu.sh
```

QEMU starts with:
- 4 vCPUs, 2 GB RAM, KVM if available
- SSH forwarded: `localhost:22222 → VM:22`
- Serial console attached to the calling terminal (Ctrl-A X to quit)

Login: `root` / `root`

**6. Deploy and test**

```sh
# From host (while QEMU is running)
rsync -avz --progress build-ratos/commrat root@localhost:/opt/commrat -e 'ssh -p 22222'

# Inside QEMU
/opt/commrat/commrat --help
```

---

### Option B — CI/release artifacts (no local build required)

Download artifacts from a GitHub release or from the
`ratos-evl-artifacts` workflow artifact on the latest `main` run.

**Files you need:**
```
vmlinuz
initrd.img
ratos-dev-image-container-amd64.ext4.gz    ← raw ext4 rootfs (new)
ratos-dev-sdk-container-amd64.*            ← SDK installer (release tags only)
```

**Boot QEMU from CI artifacts:**

```sh
# Decompress the ext4 rootfs
gunzip -k ratos-dev-image-container-amd64.ext4.gz

# Boot (KVM if available)
ACCEL=tcg; [ -w /dev/kvm ] && ACCEL=kvm
qemu-system-x86_64 \
  -cpu qemu64 -smp 4 -m 2G \
  -machine q35,accel=${ACCEL} \
  -kernel vmlinuz \
  -initrd initrd.img \
  -drive file=ratos-dev-image-container-amd64.ext4,discard=unmap,if=none,id=disk,format=raw \
  -device ide-hd,drive=disk \
  -append "root=/dev/sda rw rootwait console=ttyS0,115200 console=tty0" \
  -serial mon:stdio \
  -netdev user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
  -device virtio-net-pci,netdev=net \
  -nographic
```

**Install and use the SDK from CI:**

```sh
chmod +x ratos-dev-sdk-container-amd64.sh
./ratos-dev-sdk-container-amd64.sh   # installs to /opt/ratos-sdk by default
source /opt/ratos-sdk/environment-setup-*
```

Then cross-compile and deploy exactly as in Option A steps 4–6.

---

## QEMU tips

| Goal | Command |
|---|---|
| Quit QEMU | `Ctrl-A X` (serial monitor) |
| SSH into running VM | `ssh root@localhost -p 22222` |
| Transfer a binary | `scp -P 22222 mybinary root@localhost:/usr/local/bin/` |
| Rsync a build dir | `rsync -avz -e 'ssh -p 22222' build-ratos/ root@localhost:/opt/commrat/` |
| Check EVL version | `evl ps` or `cat /proc/evl/version` (inside VM) |
| Run with KVM | Ensure `/dev/kvm` is readable: `ls -la /dev/kvm` |

---

## SDK contents

The SDK is built from `ratos-dev-image` and includes everything set in
`xenomai-demo.conf`:

| Package | Provides |
|---|---|
| `linux-headers-xenomai-4` | EVL kernel headers (`<evl/thread.h>` etc.) |
| `libevl` | EVL user-space library headers + `libevl.so` / `libevl.a` |
| `rack-dev`, `libreflect-cpp-dev`, etc. | RaTOS development headers |

The toolchain targets the same Debian Trixie amd64 sysroot that runs
inside QEMU, so binaries built with the SDK run unmodified in the VM.

---

## Iterative development workflow (recommended)

```
code → cmake --build build-ratos → rsync to VM → run/test in QEMU → repeat
```

With KVM enabled this loop is fast:
- Cross-compilation on host: seconds
- `rsync` over SSH: sub-second for incremental changes
- No rebuilding the full rootfs image between iterations
