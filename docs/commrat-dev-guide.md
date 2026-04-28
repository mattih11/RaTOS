# CommRaT Developer Guide — QEMU + Cross-Compilation SDK

This guide covers the recommended workflow for CommRaT developers who want
to build for the RaTOS EVL target: **compile on the host** using the
cross-compilation SDK, and **run/test inside QEMU**.

## What changed recently

| Commit | Change |
|---|---|
| `a198ab5` | QEMU boot switched to raw `ext4` + `-device ide-hd` (aligns with upstream xenomai-images). `scripts/start-qemu.sh` updated accordingly. |
| `a198ab5` | `IMAGE_FSTYPES` for `container-amd64` is now `docker-archive.gz ext4 wic.gz`. The `ext4` file is used by `start-qemu.sh`; the `wic.gz` is the CI release artifact. |
| post-`a198ab5` | Console hang fixed: `console=ttyS0` only (no `console=tty0`). With dual `console=` the last entry becomes `/dev/console`; with `tty0` last, systemd sent all output to VGA (invisible under `-nographic`). Single `console=ttyS0` lets `systemd-getty-generator` auto-start `serial-getty@ttyS0`. Also switched to `-cpu host -enable-kvm` (matching upstream). `CONFIG_SATA_AHCI=y` — AHCI is built-in, no initramfs changes needed. |
| `574f4f9` | SDK build step added for version-tag CI runs. The SDK installer (`ratos-dev-sdk-container-amd64.*`) is a release asset. Compressed ext4 (`ratos-dev-image-container-amd64.ext4.gz`) also published so you can boot from CI artifacts without rebuilding. |
| (recent) | `PubkeyAuthentication yes` added to `99-ratos-dev.conf` in `ratos-dev-image.bb`. Root key-based SSH auth is now explicitly enabled in the developer image. CI and local tooling that inject an ephemeral keypair into the ext4 rootfs before QEMU boot no longer require password fallback. |

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

The SDK is a `tar.xz` rootfs written to `build/tmp/deploy/sdkchroot/`.
Extract it and run the bundled relocation script:

```sh
SDK_TAR=$(find build/tmp/deploy/sdkchroot -name '*.tar.xz' | head -1)
sudo mkdir -p /opt/ratos-sdk
sudo tar -xf "${SDK_TAR}" --strip-components=1 -C /opt/ratos-sdk
sudo /opt/ratos-sdk/relocate-sdk.sh
```

**3. Add the SDK cross-toolchain to PATH**

```sh
export PATH=/opt/ratos-sdk/usr/bin:$PATH
```

The cross-compiler is the `*-linux-gnu-gcc` / `*-linux-gnu-g++` binary in
that directory. See `/opt/ratos-sdk/README.sdk` for the full usage reference
(including the optional chroot workflow).

**4. Cross-compile CommRaT**

```sh
cd /path/to/commrat
cmake -B build-ratos \
  -DCMAKE_C_COMPILER=x86_64-linux-gnu-gcc \
  -DCMAKE_CXX_COMPILER=x86_64-linux-gnu-g++ \
  -DCMAKE_SYSROOT=/opt/ratos-sdk
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

**Files you need (available on every successful build — `ratos-evl-artifacts` or any release):**
```
vmlinuz
initrd.img
ratos-dev-image-container-amd64.ext4.gz    ← raw ext4 rootfs
```

**SDK (version-tag GitHub Releases only — not in `ratos-evl-artifacts`):**
```
ratos-dev-sdk-container-amd64.*            ← cross-compilation SDK installer
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
  -append "root=/dev/sda rw rootwait console=ttyS0" \
  -serial mon:stdio \
  -netdev user,id=net,hostfwd=tcp:127.0.0.1:22222-:22 \
  -device virtio-net-pci,netdev=net \
  -nographic
```

**Install and use the SDK from CI:**

```sh
sudo mkdir -p /opt/ratos-sdk
sudo tar -xf ratos-dev-sdk-container-amd64.tar.xz --strip-components=1 -C /opt/ratos-sdk
sudo /opt/ratos-sdk/relocate-sdk.sh
export PATH=/opt/ratos-sdk/usr/bin:$PATH
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
| `rack-dev` | RACK IPC library headers + CMake config (`RACK::rack`) |
| `libsertial-dev` | SeRTial serialization headers + CMake config (`SeRTial::sertial`) |
| `libreflect-cpp-dev` | reflect-cpp headers + CMake config (SeRTial dependency) |

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
