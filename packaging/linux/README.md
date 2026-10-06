# Linux Packaging

## Distribution Support

| Package Format | Target Distributions | Command |
|---|---|---|
| **`.deb`** | Debian, Ubuntu, Linux Mint, Pop!_OS | `./package-deb.sh` |
| **`.rpm`** | Fedora, RHEL, CentOS, Rocky Linux, openSUSE | `./package-rpm.sh` |
| **`.tar.gz` (Universal Portable)** | Arch Linux, Manjaro, Alpine, NixOS, Void Linux, all distros | `./package-tarball.sh` |

## Prerequisites
- JDK 21+ with `jpackage`
- For `.deb`: `dpkg-deb` and `fakeroot` (pre-installed on Debian/Ubuntu)
- For `.rpm`: `rpm-build` (pre-installed on Fedora/RHEL)
