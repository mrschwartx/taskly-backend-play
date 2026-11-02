FROM eclipse-temurin:21-jdk-jammy AS build
ARG SBT_VERSION=1.13.0
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" | tar -xz -C /opt
ENV PATH="/opt/sbt/bin:${PATH}"
WORKDIR /build
COPY project/build.properties project/plugins.sbt ./project/
COPY build.sbt ./
RUN sbt -batch update
COPY app ./app
COPY conf ./conf
RUN sbt -batch stage

FROM eclipse-temurin:21-jre-alpine AS runner
RUN apk add --no-cache bash
ENV APP_HOST=0.0.0.0 \
    APP_PORT=8080
WORKDIR /app
RUN addgroup -S taskly && adduser -S -G taskly taskly
COPY --from=build --chown=taskly:taskly /build/target/universal/stage ./
COPY --chown=taskly:taskly public ./public
RUN mkdir -p public/images/users && chown -R taskly:taskly /app
USER taskly
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ || exit 1
ENTRYPOINT ["bin/taskly-backend", "-Dpidfile.path=/dev/null", "-Dlogger.resource=logback-console.xml"]
