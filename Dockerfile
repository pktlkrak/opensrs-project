FROM amazoncorretto:21-alpine-jdk

COPY target/OpenSRSMVN-1.0-SNAPSHOT.jar /app/OpenSRSMVN.jar
COPY sets /app/sets
COPY common /app/common
COPY mappings /app/mappings
WORKDIR /app
CMD ["java", "-cp", "OpenSRSMVN.jar", "opensrs.OpenSRS"]

