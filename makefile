# BeanNode Build & Setup Toolchain
# Version: v0.0.4.2

VERSION=v0.0.4.2
JAR_NAME=BeanNode.jar
NODEPK=NodePK

# Main Entry Point
gBean: clean build node load-config wiz-jar copy-wiz run-wiz

# Compile All Maven Modules
build:
	mvn clean install

# Set up NodePK and copy built node JAR
node:
	if not exist $(NODEPK) mkdir $(NODEPK)
	copy BeanNode\target\BeanNode-$(VERSION).jar $(NODEPK)\$(JAR_NAME)

# Copy base config files into NodePK
load-config:
	if not exist $(NODEPK)\config.docs mkdir $(NODEPK)\config.docs
	xcopy BeanNode\config.docs $(NODEPK)\config.docs /E /I /Y

# Run the WizHelper CLI tool from inside NodePK
run-wiz:
	cd $(NODEPK) && java -cp BeanNode.jar com.beanchainbeta.devsuite.runWizCryptHelp

# Compile the devsuite helper and create a standalone CLI JAR
wiz-jar:
	javac -cp BeanNode\target\BeanNode-$(VERSION).jar -d devsuite/build ^
	devsuite\src\main\java\com\beanchainbeta\helpers\wizHelper.java ^
	devsuite\src\main\java\com\beanchainbeta\devsuite\runWizCryptHelp.java
	jar cfe devsuite\wizTool.jar com.beanchainbeta.devsuite.runWizCryptHelp -C devsuite/build .

# Copy the wizTool.jar into NodePK
copy-wiz:
	copy devsuite\wizTool.jar $(NODEPK)\

# Delete the NodePK folder (danger: hard delete!)
delete-node:
	rmdir /S /Q $(NODEPK)

# Clean all Maven builds and output folders
clean:
	mvn clean
	@if exist $(NODEPK) rmdir /S /Q $(NODEPK)

# Rebuild from scratch
rebuild: clean gBean

# Manually refresh just the node JAR (optional)
enter-node:
	copy BeanNode\target\BeanNode-$(VERSION).jar $(NODEPK)\$(JAR_NAME)

