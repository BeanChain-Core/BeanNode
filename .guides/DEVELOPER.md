# Developer Setup Guide - BeanNode

This guide explains how to use the `Makefile` toolchain to build, configure, and launch a BeanNode instance from source. It supports Linux, macOS, and Windows environments.

---

## Requirements

- Java 21+
- Maven
- Git

## Environment Setup by Platform

### Windows
Use the following to install all dev dependencies (Chocolatey, OpenJDK 21, Maven):
```powershell
make install-all
```
This command will:
- Install Chocolatey (if not installed)
- Install OpenJDK 21
- Install Maven

> Restart your terminal after running this.

### Linux (Debian/Ubuntu-based)
```bash
make install-deps-lin
```
Installs:
- OpenJDK 21
- Maven

### macOS
Make sure Homebrew is installed. Then:
```bash
make install-deps-mac
```
> To link JDK on macOS:
```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

---

## Build and Setup Commands

## Universal Commands (Linux/macOS)

| Command               | Description |
|------------------------|-------------|
| `make build`           | Clean Maven build for all modules |
| `make gBean`           | Full build + run WizCrypt (recommended) |
| `make pack`            | Full build without running WizCrypt |
| `make node`            | Copy node JAR to `NodePK/` folder |
| `make copy-crypt`      | Copy WizCrypt JAR into `NodePK` |
| `make load-config`     | Copy example configs into `NodePK/config.docs` |
| `make fresh-config`    | Reset only `beanchain.config.properties` inside `NodePK/config.docs` |
| `make run-wiz`         | Run the `WizCrypt` CLI setup tool |
| `make init-node`       | Run WizCrypt, then launch the node |
| `make launch-node`     | Launch the node JAR inside `NodePK` |
| `make prep-beans`      | Copy NodePK to `FreshBeans` (safe copy) |
| `make clean`           | Clean Maven and remove `NodePK` folder |
| `make delete-node`     | Delete `NodePK` completely |
| `make reset`           | Delete only `/data` and `/logs` inside `NodePK` |
| `make install-deps-lin`| Install OpenJDK 21 and Maven on Linux |
| `make install-deps-mac`| Install OpenJDK 21 and Maven on macOS |
| `make help`            | Show this command list |

## Windows Versions

Same as above, with `win-` prefix (where applicable):

| Command                  | Description |
|--------------------------|-------------|
| `make win-build`         | Clean Maven build for all modules |
| `make win-gBean`         | Full Windows build + WizCrypt run |
| `make win-pack`          | Full Windows build only |
| `make win-node`          | Copy node JAR to `NodePK` folder |
| `make win-copy-crypt`    | Copy WizCrypt into `NodePK` |
| `make win-load-config`   | Copy example configs into `NodePK/config.docs` |
| `make win-fresh-config`  | Reset only `beanchain.config.properties` inside `NodePK/config.docs` |
| `make win-run-wiz`       | Run WizCrypt CLI setup tool |
| `make win-init-node`     | Run WizCrypt and launch node |
| `make win-launch-node`   | Launch node JAR |
| `make win-prep-beans`    | Copy NodePK to `FreshBeans` (safe copy) |
| `make win-clean`         | Clean Maven and remove `NodePK` folder |
| `make win-delete-node`   | Hard delete `NodePK` folder |
| `make win-reset`         | Delete `/data` and `/logs` inside `NodePK` |
| `make win-switch`        | Replace Windows Makefile with Linux version |
| `make install-choco`     | Install Chocolatey (Windows package manager) |
| `make install-jdk`       | Install OpenJDK 21 via Chocolatey |
| `make install-maven`     | Install Maven via Chocolatey |
| `make install-all`       | Install all dev dependencies for Windows |
| `make win-help`          | Show this command list |

---

## Node Setup Workflow (Recommended)

1. Clone this repo:
```bash
git clone https://github.com/BeanChain-Core/BeanNode.git
cd BeanNode
```

2. Run full setup:
- **Linux/macOS:**
```bash
make gBean
```
- **Windows:**
```powershell
make win-gBean
```

3. Launch the node:
```bash
make launch-node      # or: make win-enter-node
```

4. Optional: Back up the configured node folder for deployment:
```bash
make prep-node        # or: make win-prep-beans
```

This creates a `FreshBeans/` folder containing your finalized, ready-to-deploy node setup.

---

## Additional Notes

- The `NodePK` folder is safe to delete and rebuild at any time using `make gBean`.
- Use `make reset` to clear runtime data without wiping configs or JARs.
- `WizCrypt` CLI will handle your encryption key setup and save a secure wizkey file.

---

Maintained by the **BeanChain Core Team** – Outlandish Creative LLC
