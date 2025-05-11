build:
	mvn clean install
	cp target/BeanNode-1.1-SNAPSHOT.jar docker/bootstrap/suite.jar
	cp target/BeanNode-1.1-SNAPSHOT.jar docker/peer/suite.jar
