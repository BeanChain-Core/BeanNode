# Makefile.node.win - Internal NodePK Helper for Windows

# === Launch Node JAR ===
run:
	cd ..\NodePK && java -jar BeanNode.jar

# === Run WizCrypt CLI ===
wiz:
	cd ..\NodePK && java -jar WizCrypt.jar

# === Reset Data and Logs ===
reset:
	@if exist data rmdir /S /Q data
	@if exist logs rmdir /S /Q logs

# === Help Menu ===
help:
	@echo.
	@echo ====================
	@echo NodePK Makefile Menu
	@echo ====================
	@echo make run     - Launch BeanNode
	@echo make wiz     - Run WizCrypt CLI
	@echo make reset   - Delete /data and /logs
	@echo make help    - Show this menu
	@echo.