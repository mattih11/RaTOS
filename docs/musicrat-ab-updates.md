# MusicRaT A/B Deployment on ODROID-H4

## Initial Installation

The ordinary `ratos-musicrat-image` is a single-root development image.
`ratos-musicrat-image-swupdate` adds CIP's signed SWUpdate round-robin handler,
EFI Boot Guard, a read-only SquashFS root, a persistent `/etc` overlay, and
runtime watchdog servicing.

Build from the RaTOS checkout:

```bash
scripts/build-musicrat-image.sh odroid-h4 ab
scripts/build-musicrat-installer.sh
```

The installer target includes the same A/B configuration and embeds
`ratos-musicrat-image-swupdate-ratos-odroid-h4.wic.zst`, not the single-root
MusicRaT image. Both `INSTALLER_TARGET_IMAGE` and `INSTALLER_TARGET_IMAGES`
select that payload. Installation remains attended; it must not be used until
the operator has backed up the chosen disk and approved erasing it.

Outputs under `build/tmp/deploy/images/odroid-h4/`:

| Artifact | Purpose |
| --- | --- |
| `ratos-musicrat-image-swupdate-ratos-odroid-h4.wic.gz` | Initial full-disk A/B image |
| `ratos-musicrat-image-swupdate-ratos-odroid-h4.swu` | Signed rootfs + unified kernel update for an existing A/B installation |
| `ratos-musicrat-image-swupdate-ratos-odroid-h4-ebg.swu` | Separate bootloader update; not the normal software-update bundle |
| `isar-image-installer-ratos-odroid-h4.wic.gz` | Attended installer stick with the compressed A/B payload |

Check build timestamps and checksums: deployment directories can retain old
artifacts with other suffixes. Installer multiconfig builds the target's
`.wic.zst`; build the A/B image target too when a matching `.swu` is required.

The initial disk has these GPT partitions:

| Partition | Size | Purpose |
| --- | --- | --- |
| `efi` | 16 MiB | EFI Boot Guard executable |
| `BOOT0` | 128 MiB | Slot A environment and unified kernel/initramfs |
| `BOOT1` | 128 MiB | Slot B environment and unified kernel/initramfs |
| `systema` | 2 GiB | Initial SquashFS rootfs A |
| `systemb` | 2 GiB | Initially empty rootfs B, filled by the first update |
| `var` | 512 MiB | Shared persistent state and configuration overlay |

The root partition must not expand over the inactive slot. The A/B recipes
therefore remove `expand-on-first-boot`. Remaining disk capacity is unallocated;
plan separate data storage rather than resizing an A/B root partition.

The inherited Xenomai distro assigns `WKS_FILE` after local configuration.
The shared A/B recipe include selects the A/B WKS at recipe scope so a
successful build cannot silently use the single-root layout. Validate the
resulting GPT, not just the KAS target name.

Both build helpers run `scripts/check-musicrat-ab-layout.py` against the target
disk after a successful build. It checks partition order, exact capacities,
distinct UUIDs, the initial SquashFS root, and the cleared inactive-slot header.
Its regression tests include rejection of a single-root disk:

```bash
python3 -B -m unittest discover -s scripts/tests -v
```

## Updates and Confirmation

On an already migrated A/B target, transfer the normal `.swu` to writable
storage with adequate space. `/var` has only 512 MiB by default; account for
existing data and the bundle size. Do not write bundles under the read-only
rootfs or keep old bundles indefinitely.

```bash
systemctl stop swupdate-progress.service swupdate.socket swupdate.service
swupdate -i /var/tmp/ratos-musicrat-image-swupdate-ratos-odroid-h4.swu
bg_printenv
systemctl reboot
```

This manual CLI procedure stops the daemon, its activating socket, and the
packaged progress listener first. The default `swupdate-progress -r` listener
otherwise requests an automatic reboot after a successful update. Stop the
audio graph before applying an update; do not depend on a remote connection
remaining available through installation or reboot.

SWUpdate writes the inactive root and boot slot. EFI Boot Guard then tries
the new kernel and rootfs. After reconnecting, verify the new image identity,
slot, kernel, EVL router, module lifecycle, audio, and display/touch operation.
Only then confirm:

```bash
cat /etc/os-release
cat /proc/cmdline
bg_printenv
systemctl is-active corerat-router-evl
musicrat_launcher \
  /usr/share/musicrat/examples/tone_through_processors_to_wav.json \
  --descriptor-dir /usr/share/musicrat/modules --duration-ms 300
bg_setenv -c
```

The headless WAV graph alone is not a physical-device health check. Do not
automatically confirm merely because SSH or systemd has started. An unconfirmed
trial must fall back on the next boot; the hardware watchdog is the recovery
path for a hung boot. Slot B has no valid root until the first successful update,
so the initial factory boot does not yet provide two populated fallback roots.

`/var` and the `/etc` overlay are shared between slots. Rollback does **not**
undo persistent data migrations or arbitrary configuration changes. Keep state
and schema changes backward-compatible with the previous slot and back them up.
Place MusicRaT projects and writable resources on persistent storage rather
than modifying installed example files.

## Signing and Hardware Requirements

The KAS option currently uses upstream **development snake-oil certificates**.
This permits development testing, not trusted production updates. Supply
deployment-owned certificate/key and signer providers before distributing a
production image. Private signing keys belong only in controlled build storage,
never in the image or source repository.

H4 kernel support must include SquashFS (including the selected compressor),
overlayfs, EFI variables, and the watchdog drivers. The board fragment enables
these alongside its existing audio/display/storage support. The A/B runtime
configuration asks systemd to service the watchdog at 60 seconds. Physical H4
watchdog and power-loss recovery still require target validation; QEMU uses an
emulated i6300ESB and cannot prove hardware recovery behavior.

## Validation Gates

Before physical migration:

1. Inspect the actual GPT and rootfs/kernel identity.
2. Boot through OVMF and EFI Boot Guard, not direct `-kernel` QEMU boot.
3. Apply a signed bundle for a different image UUID; verify inactive-slot
   selection and the new root after reboot.
4. Confirm a healthy trial and verify it survives another reboot.
5. Leave another trial unconfirmed and verify fallback to the confirmed slot.
6. Verify persistent state survives both update and rollback.
7. Boot the installer in an isolated VM, inspect its embedded A/B payload,
   and exercise installation only onto a disposable virtual disk.

The live H4 must not be repartitioned, flashed, or rebooted as part of these
host-side checks. Its one-time migration is a separate attended operation.

## Development Validation Record (2026-10-18)

These checks passed with the implementation in a dirty RaTOS worktree based on
`8e5eb6a6fab05e1a4729d2806094a0b7bba12a05`. They are development evidence,
not qualification of a clean published release or of physical H4 recovery.
The pinned MusicRaT/CommRaT/CoreRaT sources are unchanged from the SDK
stabilization stack; the H4 kernel was rebuilt with the A/B prerequisites.

- The A/B image and signed SWU build passed, followed by the GPT layout gate.
  Five layout regression tests passed, including rejection of the single-root
  image; shell checks passed for both build helpers.
- The disk booted through OVMF and EFI Boot Guard, with slot A mounted from
  SquashFS and systemd servicing the emulated i6300ESB watchdog at 60 seconds.
- The signed baseline bundle was rejected on the same image UUID without
  switching slots.
- A second build differing in image UUID and SWU version was signed and
  successfully applied. SWUpdate reported signature verification success,
  wrote slot B, and set its environment to `INSTALLED`. On reboot B entered
  `TESTING`; EVL sanity tests and the installed audio graph passed, then
  `bg_setenv -c` confirmed it. B survived another reboot.
- The original signed image was then installed into A and left unconfirmed.
  A deliberate kernel panic in that disposable VM stopped watchdog servicing.
  The watchdog reset the VM, EFI Boot Guard marked A `FAILED`, and the VM
  returned to confirmed B. The persistent marker under `/var/tmp`, EVL tests,
  and installed audio graph still passed.
- The attended installer booted as a virtual USB stick with only a disposable
  8 GiB virtual NVMe available as a destination. Its confirmation dialog named
  `/dev/nvme0n1`, installation reached the success dialog, and the resulting
  disk passed the A/B layout gate.
- With the installer removed, that NVMe booted through EFI Boot Guard into
  `/dev/nvme0n1p4`. Its installed EVL checks and audio graph passed. Applying
  the signed second image then booted `/dev/nvme0n1p5`, preserved a `/var`
  marker, and passed the installed graph before confirmation.
- The installer's embedded `.wic.zst` was byte-identical to the intended A/B
  payload (SHA256
  `59408df67bce4e2599081040b0f264f4bb54d45c665a1cb518ca52a36b1f2302`).

The second build used `IMAGE_UUID =
"65d56ab7-18f5-4d9c-8d0c-5738e9fc7122"` and `SWU_VERSION = "validation-2"`
only for the update/rollback test. Normal artifacts were restored afterward
with baseline UUID `919d6662-a099-438e-ac2a-4844e5ede41d`.

| Restored artifact | SHA256 |
| --- | --- |
| A/B `.wic.gz` | `8dfcf1a93d2499a272eb201bac7b67f95e0a4f8e61e7ba89651140c58e003744` |
| Normal `.swu` | `fe7c390ebfe0d460b27770a98f7db3c0bfc063998a3d5a3f37f8d8e09797f067` |
| Installer `.wic.gz` | `f1c705b231a1620e673396eca1df4216a996adfafe1b507896b2f07cb881dd7a` |

Both compressed disk artifacts passed gzip integrity checks. All validation
VMs were stopped afterward. No physical H4 deployment or reboot was performed.
Its existing SSH connection timed out during host-side builds and the mDNS
hostname became unavailable; the continued state of its live demo could not
be verified afterward.

On 2026-10-19 the operator restarted the H4. SSH connectivity and the EVL
router were verified again. The NVMe still has the original single-root
layout, and no MusicRaT demo processes were running after reboot. No migration
or disk write was performed during this check.
