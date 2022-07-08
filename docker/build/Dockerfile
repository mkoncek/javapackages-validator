FROM fedora:rawhide

RUN dnf -y install java-latest-openjdk-devel maven rpm-devel
COPY "../../pom.xml" "/usr/local/src/javapackages-validator/"

# First fetch dependencies without having sources
WORKDIR "/usr/local/src/javapackages-validator"
RUN JAVA_HOME=/usr/lib/jvm/java-18 mvn clean install

COPY "../../src/" "/usr/local/src/javapackages-validator/src/"
RUN JAVA_HOME=/usr/lib/jvm/java-18 mvn clean install

COPY "../../run.sh" "/usr/local/src/javapackages-validator/"

ENTRYPOINT ["./run.sh"]
