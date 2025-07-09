############################################################
# BeanNode Build & Setup Toolchain - Multi-Platform Manual
# Version: v0.1.0
# Author: Outlandish Creative / BeanChain Core Team
############################################################

VERSION       = v0.1.0
NODE_MODULE   = BeanNode
DEV_MODULE    = WizCrypt
NODEPK        = NodePK
FRESHB		  = FreshBeans
NODE_JAR      = $(NODE_MODULE)-$(VERSION).jar
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
	@echo # BeanNode Toolchain - Linux/macOS Command Reference
	@echo ############################################################

	@echo
	@echo === Core Build Commands ===
	@echo make gBean             - Full build w/WizCrypt, config, and run DevSuite
	@echo make pack              - Full build: node JAR, DevSuite, and config
	@echo make build             - Clean Maven build of all modules

	@echo
	@echo === File/Config Management ===
	@echo make node              - Copy node JAR to NodePK folder
	@echo make copy-crypt        - Copy WizCrypt JAR to NodePK folder
	@echo make load-config       - Copy config.docs into NodePK folder
	@echo make fresh-config      - Reset only the beanchain.config.properties file
	@echo make prep-beans        - Copy NodePK to FreshBeans (safe copy)

	@echo
	@echo === Run / Launch ===
	@echo make run-wiz           - Run the WizCrypt CLI JAR from NodePK
	@echo make init-node         - Run WizCrypt, then launch node
	@echo make launch-node       - Launch the node JAR from NodePK

	@echo
	@echo === Cleanup and Reset ===
	@echo make clean             - Clean build and delete NodePK folder
	@echo make delete-node       - Force delete NodePK folder
	@echo make reset             - Delete NodePK/data and NodePK/logs

	@echo
	@echo === Dev Environment Setup ===
	@echo make install-deps-lin  - Install Java and Maven on Linux
	@echo make install-deps-mac  - Install Java and Maven on macOS

	@echo
	@echo === Help ===
	@echo make help              - Show this help list
	@echo

# === Command Help (Windows) ===
win-help:
	@echo ############################################################
	@echo # BeanNode Toolchain - Windows Command Reference
	@echo ############################################################
	@echo.
	@echo === Core Build Commands ===
	@echo make win-gBean         - Full build w/WizCrypt, config, and run DevSuite
	@echo make win-pack          - Full build: node JAR, DevSuite, and config
	@echo make win-build         - Clean Maven build of all modules
	@echo.
	@echo === File/Config Management ===
	@echo make win-node          - Copy node JAR to NodePK folder
	@echo make win-copy-crypt    - Copy WizCrypt JAR to NodePK folder
	@echo make win-load-config   - Copy config.docs into NodePK folder
	@echo make win-fresh-config  - Reset only the beanchain.config.properties file
	@echo make win-prep-beans    - Copy NodePK to FreshBeans (safe copy)
	@echo.
	@echo === Run / Launch ===
	@echo make win-run-wiz       - Run the WizCrypt CLI JAR from NodePK
	@echo make win-init-node     - Run WizCrypt, then launch node
	@echo make win-launch-node   - Launch the node JAR from NodePK
	@echo.
	@echo === Cleanup and Reset ===
	@echo make win-clean         - Clean build and delete NodePK folder
	@echo make win-delete-node   - Force delete NodePK folder
	@echo make win-reset         - Delete NodePK/data and NodePK/logs
	@echo.
	@echo === Dev Environment Setup ===
	@echo make install-choco     - Install Chocolatey (Windows package manager)
	@echo make install-jdk       - Install OpenJDK 21 via Chocolatey
	@echo make install-maven     - Install Maven via Chocolatey
	@echo make install-all       - Install all Windows dev dependencies
	@echo.
	@echo === Help ===
	@echo make win-switch        - Replace NodePK Makefile with LINUX version
	@echo make win-help          - Show this help list
	@echo.

# === Commands (Linux/macOS as Default) ===
gBean: pack run-wiz
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
	cp .beanies\makefile.node $(NODEPK)/Makefile
	cp -r $(NODE_MODULE)/config.docs/* $(NODEPK)/config.docs

fresh-config:
	rm -rf $(NODEPK)\config.docs\beanchain.config.properties
	cp -r BeanNode\config.docs\beanchain.config.properties $(NODEPK)\config.docs\beanchain.config.properties

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
win-gBean: win-pack win-run-wiz
win-pack: win-clean win-build win-node win-load-config win-copy-crypt

win-serve: win-pack win-switch win-dewiz

win-build:
	mvn clean install

win-node:
	@if not exist $(NODEPK) mkdir $(NODEPK)
	copy $(NODE_JAR_PATH_WIN) $(NODEPK)\$(NODE_JAR)

win-copy-crypt:
	copy $(SUITE_JAR_PATH_WIN) $(NODEPK)\$(SUITE_JAR)

win-load-config:
	@if not exist $(NODEPK)\config.docs mkdir $(NODEPK)\config.docs
	copy .beanies\makefile.node.win $(NODEPK)\Makefile
	xcopy $(NODE_MODULE)\config.docs $(NODEPK)\config.docs /E /I /Y

win-switch:
	@if exist $(NODEPK)\Makefile del /F $(NODEPK)\Makefile
	copy .beanies\makefile.node $(NODEPK)\Makefile

win-fresh-config:
	@if exist $(NODEPK)\config.docs\beanchain.config.properties del /F $(NODEPK)\config.docs\beanchain.config.properties
	copy BeanNode\config.docs\beanchain.config.properties $(NODEPK)\config.docs\beanchain.config.properties

win-run-wiz:
	cd $(NODEPK) && java -jar $(SUITE_JAR)

win-init-node:
	make win-run-wiz
	make win-launch-node

win-launch-node:
	cd $(NODEPK) && java -jar $(NODE_JAR)

#need to add the removal of the wizCrypt version of this to slim server launch folder
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



# === devTeam Commands for quick building (secret non menu) ===

win-swapfresh:
	@if exist $(NODEPK)\config.docs rmdir /S /Q $(NODEPK)\config.docs
	mkdir $(NODEPK)\config.docs
	@if exist $(FRESHB)\config.docs xcopy $(FRESHB)\config.docs $(NODEPK)\config.docs /E /I /Y

pop:
	win-pack win-swapfresh win-launch-node

win-dewiz:
	@if exist NodePK\WizCrypt.jar del /F NodePK\WizCrypt.jar





# === in testing 

# ===  make ./bean replace make ===

init-win:
	@echo @echo off > bean.bat
	@echo make %%* >> bean.bat
	@echo Created bean.bat for Windows

init-unix:
	@echo '#!/bin/bash' > bean
	@echo 'make "$$@"' >> bean
	@chmod +x bean
	@echo Created bean script for Linux/macOS


# ===TEAM COMMANDS===

win-gpn:
	@if exist $(FRESHB) xcopy $(FRESHB) TEAM\GPNTEST /E /I /Y


