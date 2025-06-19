build:
	mvn clean install
	copy .\target\BeanNode-v0.0.0.jar .\docker\bootstrap\suite.jar
	copy .\target\BeanNode-v0.0.0.jar .\docker\peer\suite.jar
