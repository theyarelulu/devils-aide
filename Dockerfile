FROM gradle:latest

WORKDIR /DevilsAide/
COPY ./ ./
RUN gradle jar

FROM ubuntu:latest
RUN apt update && apt install -y default-jre
WORKDIR /root/
COPY --from=0 /DevilsAide/build/libs/DevilsAide.jar ./
ENTRYPOINT java -jar /root/DevilsAide.jar
EXPOSE 8080