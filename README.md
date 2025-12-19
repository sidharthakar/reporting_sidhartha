# Allure Dashboard - Local & S3 switchable

## Build
mvn clean package

## Run locally (filesystem mode)
java -jar target/allure-platform-0.0.1-SNAPSHOT.jar

## Run locally with explicit local storage property
java -Dstorage.mode=local -jar target/allure-platform-0.0.1-SNAPSHOT.jar

## Run in S3 mode (requires AWS credentials)
export AWS_REGION=ap-south-1
export AWS_S3_BUCKET=your-bucket
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
java -Dstorage.mode=s3 -jar target/allure-platform-0.0.1-SNAPSHOT.jar

## Docker
docker build -t allure-dashboard .
docker run -p 8080:8080 -e STORAGE_MODE=local allure-dashboard

# Notes
- In local mode artifacts stored under `app.storage.root` (default `storage`)
- In s3 mode artifacts are uploaded to S3 under `reports/{app}/{release}/{runId}/`


