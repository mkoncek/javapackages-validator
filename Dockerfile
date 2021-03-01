# Build

FROM fedora:33

RUN dnf -y install maven git-core

RUN git clone "https://github.com/mizdebsk/java-deptools-native.git"
RUN git clone "https://github.com/fedora-java/javapackages-validator.git"

RUN cd "java-deptools-native" && mvn dependency:go-offline
RUN cd "java-deptools-native" && mvn compile
RUN cd "java-deptools-native" && mvn install

RUN cd "javapackages-validator" && mvn dependency:go-offline
RUN cd "javapackages-validator" && mvn compile
RUN cd "javapackages-validator" && mvn install

# Runtime

FROM ubi8

RUN dnf -y install java-11-openjdk-headless

COPY --from=0 "/javapackages-validator/target/" "/"

CMD ["java", "-jar", "/assembly-0.1/validator-0.1.jar -c /config"]
