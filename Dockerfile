# Build

FROM fedora:33

RUN dnf -y install maven git-core

RUN git -C "/opt" clone "https://github.com/mizdebsk/java-deptools-native.git"
RUN git -C "/opt" clone "https://github.com/fedora-java/javapackages-validator.git"

RUN cd "/opt/java-deptools-native" && mvn dependency:go-offline
RUN cd "/opt/java-deptools-native" && mvn compile
RUN cd "/opt/java-deptools-native" && mvn install

RUN cd "/opt/javapackages-validator" && mvn dependency:go-offline
RUN cd "/opt/javapackages-validator" && mvn compile
RUN cd "/opt/javapackages-validator" && mvn install

# Runtime

FROM ubi8

RUN dnf -y install java-11-openjdk-headless

COPY --from=0 "/opt/javapackages-validator/target/" "/opt/"

CMD ["java", "-jar", "/opt/assembly-0.1/validator-0.1.jar", "-h"]
