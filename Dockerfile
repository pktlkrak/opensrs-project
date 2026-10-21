FROM amazoncorretto:21-alpine-jdk

COPY target/OpenSRSMVN-1.0-SNAPSHOT.jar /app/OpenSRSMVN.jar
COPY sets /app/sets
COPY common /app/common
COPY mappings /app/mappings
WORKDIR /app
CMD exec java \
    -DPOSTGRES_USER="$POSTGRES_USER" \
    -DPOSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
    -DPOSTGRES_DB="$POSTGRES_DB" \
    -DPOSTGRES_HOST="$POSTGRES_HOST" \
    -DPOSTGRES_PORT="${POSTGRES_PORT}" \
    -cp OpenSRSMVN.jar opensrs.OpenSRS
