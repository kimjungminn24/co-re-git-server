FROM ubuntu:22.04

RUN apt update && \
    apt install -y git openssh-server openjdk-17-jdk curl unzip && \
    useradd -m -s /bin/bash gituser && \
    mkdir /var/run/sshd && \
    mkdir -p /home/gituser/repos && \
    chown -R gituser:gituser /home/gituser

EXPOSE 22 8080

COPY build/libs/git-server.jar /app/git-server.jar

RUN mkdir -p /home/gituser/.ssh && \
    chmod 700 /home/gituser/.ssh && \
    chown -R gituser:gituser /home/gituser/.ssh

CMD ["sh", "-c", "service ssh start && java -jar /app/git-server.jar"]
