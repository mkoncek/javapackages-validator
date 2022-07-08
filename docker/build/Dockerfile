FROM fedora:rawhide

RUN dnf -y install java-latest-openjdk-devel maven rpm-devel
COPY "../../pom.xml" "/usr/local/src/javapackages-validator/"
COPY "../../src/" "/usr/local/src/javapackages-validator/"

WORKDIR "/usr/local/src/javapackages-validator"
RUN JAVA_HOME=/usr/lib/jvm/java-18 mvn install

COPY "../../run.sh" "/usr/local/src/javapackages-validator/"

ENTRYPOINT ["./run.sh"]
