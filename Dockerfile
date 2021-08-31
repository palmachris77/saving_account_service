FROM openjdk:8
VOLUME /tmp 
ADD ./target/saving-account-0.0.1-SNAPSHOT.jar service-saving-account.jar
ENTRYPOINT [ "java", "-jar","./service-saving-account.jar" ]