FROM docker.io/java:openjdk-8-jdk
ADD target/onebusaway-gtfs-realtime-visualizer-0.0.1-SNAPSHOT.jar /opt/
ENV GTFS_SOURCE "--vehiclePositionsUrl=https://gtfsrt.api.translink.com.au/Feed/SEQ"
EXPOSE 8080
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar /opt/onebusaway-gtfs-realtime-visualizer-0.0.1-SNAPSHOT.jar $GTFS_SOURCE"]
