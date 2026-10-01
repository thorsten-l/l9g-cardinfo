FROM bellsoft/liberica-openjre-alpine:25
WORKDIR /
# data/config.yaml and data/secret.bin are read relative to the working directory
VOLUME [ "/data" ]
COPY l9g-cardinfo.jar /l9g-cardinfo.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:9000/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/l9g-cardinfo.jar"]
