# Layered Development Guide — RaTOS / ISAR

This document explains how to structure incremental build targets so that
developers working on a specific component (EVL, CoreRaT, CommRaT, …) can
build and test **exactly what they need** without always rebuilding the full
RaTOS image.

---

## Background: what "layer" means in ISAR / Yocto

"Layer" is an overloaded word.  In ISAR / BitBake it means two distinct things:

| Concept | What it is | Where it appears |
|---|---|---|
| **BitBake metadata layer** | A directory with a `conf/layer.conf`; holds recipes, classes, config fragments. Listed in `BBLAYERS`. | `meta-ratos/`, `isar/meta/`, `isar-cip-core/`, … |
| **Image variant / build target** | A `.bb` image recipe that `IMAGE_INSTALL`s a specific package set. | `recipes-core/images/*.bb` |

Both are relevant here.  The user-facing workflow is controlled by **KAS
target fragments** that select which image recipe to build.

---

## The dependency stack

```
EVL (kernel + libevl)          ← needed by: CoreRaT
  └── CoreRaT                  ← needed by: CommRaT
        └── CommRaT            ← needed by: full RaTOS image / end-user apps
              └── ratos-image (end-user)
```

BitBake already knows these relationships through `DEPENDS =` in each recipe.
When you build `ratos-commrat-image`, BitBake only compiles EVL, CoreRaT, and
CommRaT — it never touches app-level recipes that are not in the dependency
tree.

---

## Solution A — Image recipe hierarchy (implemented)

One image recipe per integration level in `recipes-core/images/`.  Each recipe
is self-contained and lists only the packages it needs.

### Recipe hierarchy

```
recipes-core/images/
  ratos-container-qemu-setup.inc   # shared: hostname / networkd / sshd setup
  ratos-evl-image.bb               # layer 1: EVL kernel + libevl
  ratos-corerat-image.bb           # layer 2: layer 1 + CoreRaT
  ratos-commrat-image.bb           # layer 3: layer 2 + CommRaT
  ratos-image.bb                   # layer 4: full production image (unchanged)
  ratos-dev-image.bb               # dev container with build toolchain (unchanged)
```

`ratos-container-qemu-setup.inc` provides three `ROOTFS_POSTPROCESS_COMMAND`
functions that all layered images share:
- `set_hostname` — writes `/etc/hostname` from the recipe's `HOSTNAME` variable
- `configure_networking` — creates a systemd-networkd `10-dhcp.network` for `en*`
- `configure_sshd` — drops `99-ratos-dev.conf` permitting root SSH login (dev only)

Each layered image recipe ends with:
```bitbake
require recipes-core/images/ratos-container-qemu-setup.inc
```

> **Why not `require` a parent image recipe?**
> ISAR's `image.bbclass` does not offer a clean "extend this image" mechanism.
> Each recipe is self-contained (`DEPENDS` + `IMAGE_INSTALL`) to avoid
> unexpected `do_rootfs` interactions from inherited side effects.

---

## Solution B — KAS target fragments (implemented)

KAS fragments in `kas/target/` override the `target:` field set by the board
fragment.  The board sets the machine; the target fragment sets the image.

```
kas/target/
  evl.yaml      # target: ratos-evl-image
  corerat.yaml  # target: ratos-corerat-image
  commrat.yaml  # target: ratos-commrat-image
```

### Build commands

```sh
# Layer 1: EVL base — QEMU (for CoreRaT development)
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/evl.yaml

# Layer 2: EVL + CoreRaT — QEMU (for CommRaT development)
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/corerat.yaml

# Layer 3: EVL + CoreRaT + CommRaT — QEMU (for app development)
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml:kas/target/commrat.yaml

# Layer 4: full image — QEMU dev container (unchanged)
kas-container --isar build kas.yaml:kas/board/container-amd64.yaml

# Any layer on Odroid H4 hardware
kas-container --isar build kas.yaml:kas/board/odroid-h4.yaml:kas/target/corerat.yaml
```

A defines *what* gets built; B provides the convenient CLI selection of *which*
image to build.  Both are implemented in the repository.

---

## Solution C — Separate BitBake metadata layers (future work)

This section describes how to split the monorepo into separate metadata layers
when CoreRaT or CommRaT need independent release cadences or are owned by
separate teams.  **This is not yet implemented** — Solutions A and B above are
what is in the repository today.

### Proposed structure

```
meta-ratos-base/          (this repo, minus app-specific recipes)
  conf/layer.conf
  recipes-bsp/            EVL patches, u-boot
  recipes-xenomai/        libevl
  recipes-core/images/    ratos-evl-image.bb
  recipes-reflect-cpp/
  recipes-sertial/

meta-ratos-corerat/       (separate git repo, possibly in the CoreRaT project)
  conf/layer.conf
  recipes-corerat/        corerat_git.bb
  recipes-core/images/    ratos-corerat-image.bb

meta-ratos-commrat/       (separate git repo, possibly in the CommRaT project)
  conf/layer.conf
  recipes-commrat/        commrat_git.bb
  recipes-core/images/    ratos-commrat-image.bb

meta-ratos-apps/          (end-user integration layer)
  conf/layer.conf
  recipes-core/images/    ratos-image.bb
  recipes-apps/           …
```

Each repo is registered in `kas.yaml` under `repos:` and its directory is
added to `layers:`:

```yaml
repos:
  ratos:
    layers:
      .:                            # meta-ratos-base

  meta-ratos-corerat:
    url: https://github.com/…/meta-ratos-corerat.git
    commit: <pin>
    layers:
      .:

  meta-ratos-commrat:
    url: https://github.com/…/meta-ratos-commrat.git
    commit: <pin>
    layers:
      .:
```

A developer working on CoreRaT can use a local override:

```yaml
# kas/local/corerat-local.yaml  (git-ignored, personal override)
header:
  version: 14

repos:
  meta-ratos-corerat:
    path: /home/dev/src/CoreRaT/meta-ratos-corerat   # local checkout
```

```sh
kas-container --isar build \
  kas.yaml:kas/board/container-amd64.yaml:kas/target/corerat.yaml:kas/local/corerat-local.yaml
```

---

## Which approach should you use?

| Situation | Approach |
|---|---|
| Small team, monorepo, want faster test builds now | **A + B** (image recipes + KAS fragments) |
| CoreRaT / CommRaT have independent versioning or are in separate repos | **C** (separate metadata layers) + KAS `repos:` pinning |
| Both of the above | **A + B + C** — they compose cleanly |

### Key point

In ISAR/Yocto, "layering" for development purposes is primarily a **BitBake
dependency + image recipe** concern, not a repository structure concern.  The
correct incremental development workflow is:

1. Define separate image recipes that describe exactly the package set for each
   development stage.
2. Use KAS target fragments to select the desired image on the command line.
3. Optionally split metadata into separate repos when team / release boundaries
   require it.

Separating into different repos does **not** automatically give you faster
builds — BitBake's shared state cache (`sstate-cache`) already avoids
re-building unchanged recipes regardless of which image recipe is targeted.
The main benefit of separate repos is **independent versioning** and
**reduced blast radius** when a recipe change in one layer cannot accidentally
affect another layer's build.

---

## Summary of the four build levels

| Build level | Image recipe | What gets compiled | Typical user |
|---|---|---|---|
| 1 — EVL base | `ratos-evl-image` | EVL kernel, libevl, sertial, reflect-cpp | CoreRaT developer, EVL bringup |
| 2 — CoreRaT | `ratos-corerat-image` | Level 1 + CoreRaT | CommRaT developer |
| 3 — CommRaT | `ratos-commrat-image` | Level 2 + CommRaT | App developer / integration |
| 4 — Full | `ratos-image` | Level 3 + end-user apps | QA, release, production |
