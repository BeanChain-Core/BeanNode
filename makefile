############################################################
# BeanNode Build & Setup Toolchain - Multi-Platform Manual
# Version: v0.0.4.2
# Author: Outlandish Creative / BeanChain Core Team
############################################################

VERSION       = v0.0.4.2
NODE_MODULE   = BeanNode
DEV_MODULE    = WizCrypt
NODEPK        = NodePK
NODE_JAR      = $(NODE_MODULE).jar
SUITE_JAR     = WizCrypt.jar

# === Platform-Specific Path Definitions ===
NODE_JAR_PATH_WIN  = $(NODE_MODULE)\target\$(NODE_MODULE)-$(VERSION).jar
SUITE_JAR_PATH_WIN = $(DEV_MODULE)\target\$(SUITE_JAR)

NODE_JAR_PATH_UNIX = $(NODE_MODULE)/target/$(NODE_MODULE)-$(VERSION).jar
SUITE_JAR_PATH_UNIX = $(DEV_MODULE)/target/$(SUITE_JAR)


# === Command Help (Linux/macOS) ===
help:
	@echo
	@echo ############################################################
	@echo # BeanNode Build and Setup Toolchain - Linux/macOS Commands
	@echo ############################################################
	@echo
	@echo make gBean         - Full build w/WizCrpyt: node JAR, DevSuite, config, and run DevSuite
	@echo make pack          - Full build: node JAR, DevSuite, config
	@echo make build         - Clean Maven build of all modules
	@echo make node          - Copy node JAR to NodePK folder
	@echo make copy-crypt    - Copy WizCrypt JAR to NodePK folder
	@echo make load-config   - Copy config.docs into NodePK folder
	@echo make run-wiz       - Run the WizCrypt CLI JAR from NodePK
	@echo make init-node     - Run WizCrpyt then Launch Node
	@echo make launch-node   - Launch the node JAR inside NodePK
	@echo make clean         - Maven clean and remove NodePK folder
	@echo make delete-node   - Force delete NodePK folder
	@echo make reset         - Delete /data and /logs from NodePK folder
	@echo make help          - Show this command list
	@echo

# === Command Help (Windows) ===
win-help:
	@echo.
	@echo ############################################################
	@echo # BeanNode Build and Setup Toolchain - Windows Commands
	@echo ############################################################
	@echo.
	@echo make win-gBean         - Full build w/WizCrpyt: node JAR, DevSuite, config, and run DevSuite
	@echo make win-pack          - Full build: node JAR, DevSuite, config
	@echo make win-build         - Clean Maven build of all modules
	@echo make win-node          - Copy node JAR to NodePK folder
	@echo make win-copy-crypt    - Copy WizCrypt JAR to NodePK folder
	@echo make win-load-config   - Copy config.docs into NodePK folder
	@echo make win-run-wiz       - Run the WizCrypt CLI JAR from NodePK
	@echo make win-init-node     - Run WizCrpyt then Launch Node
	@echo make win-enter-node    - Launch the node JAR inside NodePK
	@echo make win-clean         - Maven clean and remove NodePK folder
	@echo make win-delete-node   - Force delete NodePK folder
	@echo make win-reset         - Delete /data and /logs from NodePK folder
	@echo make win-help          - Show this command list
	@echo.

# === Commands (Linux/macOS as Default) ===
gBean: clean build node load-config copy-crypt run-wiz
pack: clean build node load-config copy-crypt

build:
	mvn clean install

node:
	mkdir -p $(NODEPK)
	cp -f $(NODE_JAR_PATH_UNIX) $(NODEPK)/$(NODE_JAR)

copy-crypt:
	cp -f $(SUITE_JAR_PATH_UNIX) $(NODEPK)/$(SUITE_JAR)

load-config:
	mkdir -p $(NODEPK)/config.docs
	cp makefile.node $(NODEPK)/Makefile
	cp -r $(NODE_MODULE)/config.docs/* $(NODEPK)/config.docs

run-wiz:
	cd $(NODEPK) && java -jar $(SUITE_JAR)

init-node:
	make run-wiz
	make launch-node

launch-node:
	cd $(NODEPK) && java -jar $(NODE_JAR)

prep-beans:
	cp -f $(NODEPK) FreshBeans 

clean:
	mvn clean
	rm -rf $(NODEPK)

delete-node:
	rm -rf $(NODEPK)

reset:
	rm -rf $(NODEPK)/data $(NODEPK)/logs

# === Windows-Specific Versions ===
win-gBean: win-clean win-build win-node win-load-config win-copy-crypt win-run-wiz
win-pack: win-clean win-build win-node win-load-config win-copy-crypt

win-build:
	mvn clean install

win-node:
	@if not exist $(NODEPK) mkdir $(NODEPK)
	copy $(NODE_JAR_PATH_WIN) $(NODEPK)\$(NODE_JAR)

win-copy-crypt:
	copy $(SUITE_JAR_PATH_WIN) $(NODEPK)\$(SUITE_JAR)

win-load-config:
	@if not exist $(NODEPK)\config.docs mkdir $(NODEPK)\config.docs
	copy makefile.node.win $(NODEPK)\Makefile
	xcopy $(NODE_MODULE)\config.docs $(NODEPK)\config.docs /E /I /Y

win-run-wiz:
	cd $(NODEPK) && java -jar $(SUITE_JAR)

win-init-node:
	make win-run-wiz
	make win-launch-node

win-launch-node:
	cd $(NODEPK) && java -jar $(NODE_JAR)

win-prep-beans:
	xcopy $(NODEPK) FreshBeans /E /I /Y

win-clean:
	mvn clean
	@if exist $(NODEPK) rmdir /S /Q $(NODEPK)

win-delete-node:
	rmdir /S /Q $(NODEPK)

win-reset:
	@if exist $(NODEPK)\data rmdir /S /Q $(NODEPK)\data
	@if exist $(NODEPK)\logs rmdir /S /Q $(NODEPK)\logs

# === Windows Dev Env Setup ===
install-choco:
	@echo Checking for Chocolatey...
	@if exist C:\ProgramData\chocolatey (
		echo Chocolatey already installed.
	) else (
		@powershell -NoProfile -ExecutionPolicy Bypass -Command "Set-ExecutionPolicy Bypass -Scope Process; [System.Net.ServicePointManager]::SecurityProtocol = 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))"
	)

install-jdk:
	@echo Installing OpenJDK 21...
	choco install openjdk --version=21.0.2 -y

install-maven:
	@echo Installing Maven...
	choco install maven -y

install-all:
	make install-choco
	make install-jdk
	make install-maven
	@echo Dev environment ready. You may need to restart your terminal.

# === Linux Dev Env Setup ===
install-deps-lin:
	sudo apt update && sudo apt install openjdk-21-jdk maven -y

# === macOS Dev Env Setup ===
install-deps-mac:
	brew update && brew install openjdk@21 maven
	@echo 'To link JDK on macOS:'
	@echo 'sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk'
