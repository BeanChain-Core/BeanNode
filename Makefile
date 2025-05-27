build:
	mvn clean install
	copy .\target\BeanNode-1.1-SNAPSHOT.jar .\docker\bootstrap\suite.jar
	copy .\target\BeanNode-1.1-SNAPSHOT.jar .\docker\peer\suite.jar
