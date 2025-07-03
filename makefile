############################################################
# BeanNode Build & Setup Toolchain
# Version: v0.0.4.2
# Author: Outlandish Creative / BeanChain Core Team
############################################################

# === Version Config ===

VERSION       = v0.0.4.2


# === Module Names ===

NODE_MODULE   = BeanNode
DEV_MODULE    = WizCrypt
NODEPK        = NodePK
NODE_JAR      = $(NODE_MODULE).jar
SUITE_JAR     = WizCrypt.jar

# === File Paths ===

NODE_JAR_PATH  = $(NODE_MODULE)\target\$(NODE_MODULE)-$(VERSION).jar
SUITE_JAR_PATH = $(DEV_MODULE)\target\$(SUITE_JAR)

# === List All Wiondows Commands ===

win-help:
	@echo.
	@echo ############################################################
	@echo # BeanNode Build and Setup Toolchain - Command Reference
	@echo ############################################################
	@echo.
	@echo make gBean         - Full build w/WizCrpyt: node JAR, DevSuite, config, and run DevSuite
	@echo make pack          - Full build: node JAR, DevSuite, config
	@echo make build         - Clean Maven build of all modules
	@echo make node          - Copy node JAR to NodePK folder
	@echo make copy-crypt    - Copy WizCrypt JAR to NodePK folder
	@echo make load-config   - Copy config.docs into NodePK folder
	@echo make run-wiz       - Run the WizCrypt CLI JAR from NodePK
	@echo make init-node     - Run WizCrpyt then Launch Node
	@echo make enter-node    - Launch the node JAR inside NodePK
	@echo make clean         - Maven clean and remove NodePK folder
	@echo make delete-node   - Force delete NodePK folder
	@echo make rebuild       - Clean and full rebuild using gBean
	@echo make reset         - Delete /data and /logs from NodePK folder
	@echo make help          - Show this command list
	@echo.

# === Default Commands ===

gBean: clean depend build node load-config copy-suite run-wiz

pack: clean depend build node load-config copy-crypt

# === Maven Build ===

build:
	mvn clean install

# === Prepare DevSuite Source Dependency ===

depend:
	@if not exist "DevSuite\src\main\java\io\beanchain\devsuite" mkdir "DevSuite\src\main\java\io\beanchain\devsuite"
	copy /Y "BeanNode\src\main\java\io\beanchain\devsuite\runWizCryptHelp.java" "DevSuite\src\main\java\io\beanchain\devsuite\"
	copy /Y "BeanNode\src\main\java\io\beanchain\devsuite\WizHelperLite.java" "DevSuite\src\main\java\io\beanchain\devsuite\"

# === Copy Node JAR into NodePK ===

node:
	if not exist $(NODEPK) mkdir $(NODEPK)
	copy $(NODE_JAR_PATH) $(NODEPK)\$(NODE_JAR)

# === Copy DevSuite Fat JAR into NodePK ===

copy-crypt:
	copy $(SUITE_JAR_PATH) $(NODEPK)\$(SUITE_JAR)

# === Copy config.docs into NodePK ===

load-config:
	if not exist $(NODEPK)\config.docs mkdir $(NODEPK)\config.docs
	xcopy $(NODE_MODULE)\config.docs $(NODEPK)\config.docs /E /I /Y

# === Run DevSuite CLI Tool ===

run-wiz:
	cd $(NODEPK) && java -jar $(SUITE_JAR)

# === Clean Build ===

clean:
	mvn clean
	@if exist $(NODEPK) rmdir /S /Q $(NODEPK)

# === Manual Delete Only ===

delete-node:
	rmdir /S /Q $(NODEPK)

# === Force Full Rebuild ===

rebuild: clean gBean

# === Launch Node In NodePK ===

init-node: 
	make run-wiz
	make launch-node

launch-node:
	cd $(NODEPK) && java -jar $(NODE_JAR)

reset-node:
	cd $(NODEPK) && make reset


