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


EADME.md (FOLLOW THIS)
# Allure Dashboard

## Local Run
```bash
mvn clean package
java -jar target/allure-platform.jar


UI → http://localhost:8080

AWS Deployment (ECR + S3)
1. Build Docker Image
docker build -t allure-dashboard .

2. Push to ECR
aws ecr create-repository --repository-name allure-dashboard
docker tag allure-dashboard:latest <ECR_URI>:latest
docker push <ECR_URI>:latest

3. S3 Bucket

Create bucket:

aws s3 mb s3://allure-dashboard-prod

4. Environment Variables
STORAGE_MODE=s3
AWS_REGION=ap-south-1
AWS_S3_BUCKET=allure-dashboard-prod

5. Run on EC2 / ECS
docker run -d -p 80:8080 --env-file .env <ECR_URI>

Supported Features

Local + S3 storage

Release trends

App overview

Single HTML report

Grafana-like charts


