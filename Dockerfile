    FROM amazoncorretto:17

    WORKDIR /app

    RUN yum update -y && yum install -y wget unzip && yum clean all

    ARG ALLURE_VERSION=2.27.0
    RUN wget https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.tgz -O /tmp/allure.tgz \
      && mkdir -p /opt/allure \
      && tar -xzf /tmp/allure.tgz -C /opt \
      && mv /opt/allure-${ALLURE_VERSION} /opt/allure \
      && ln -s /opt/allure/bin/allure /usr/local/bin/allure

    ARG JAR_FILE=target/*.jar
    COPY ${JAR_FILE} app.jar

    ENV STORAGE_MODE=local
    ENV AWS_REGION=ap-south-1
    ENV AWS_S3_BUCKET=allure-dashboard-prod

    EXPOSE 8080

    ENTRYPOINT ["java","-jar","/app/app.jar"]
