
# 🚀 Centralized Allure Reporting Platform

<p align="center">
  <img src="https://img.shields.io/badge/Status-Active-success?style=flat" />
  <img src="https://img.shields.io/badge/Backend-SpringBoot-blue" />
  <img src="https://img.shields.io/badge/Frontend-VanillaJS-yellow" />
  <img src="https://img.shields.io/badge/Storage-S3-orange" />
  <img src="https://img.shields.io/badge/Deployment-Docker%20%7C%20ECR-blueviolet" />
</p>

---

## 📌 Project Overview

**Centralized Allure Reporting System** is a scalable, self-hosted solution to **upload, store, visualize, and analyze Allure test reports** across multiple applications and releases.

It is designed for:

* QA teams
* Automation engineers
* DevOps teams
* Organizations needing centralized test visibility

---

## 🎯 Key Features

✅ Upload Allure ZIP reports
✅ Store reports in **S3 / Local filesystem**
✅ Auto-generate **Allure HTML reports**
✅ **Execution-date–based analytics**
✅ App-level & Release-level filtering
✅ Historical trend comparison
✅ Secure API-based access
✅ Works with **Docker + AWS (ECR / S3)**
✅ Clean UI (HTML + JS + Chart.js)

---

## 🧩 Architecture Overview

```
┌──────────────┐
│  Test Runner │
│ (CI / Local) │
└──────┬───────┘
       │ ZIP Upload
       ▼
┌─────────────────────────┐
│  Spring Boot API Server │
│  - Upload API           │
│  - Analytics Engine     │
│  - Report Generator     │
└──────┬──────────────────┘
       │
       ▼
┌───────────────┐        ┌──────────────────┐
│  AWS S3       │◀──────▶│  Local Storage   │
│  (Reports)    │        │  (Optional)      │
└───────────────┘
       │
       ▼
┌──────────────────────┐
│   Web Dashboard UI   │
│ (Charts + Viewer)    │
└──────────────────────┘
```

---

## 🧪 Features in Detail

### 📤 Upload

* Upload zipped **Allure results**
* Choose:

    * App Name
    * Release
    * Execution Date
* Auto-generates:

    * HTML report
    * Trend data
    * Historical linkage

---

### 📊 Analytics

* App-level overview
* Release-level trends
* Date range filtering
* Chart types:

    * Passed / Failed / Broken / Skipped
    * Pass percentage

---

### 📁 Report Management

* View report directly in browser
* Download HTML
* Delete:

    * Single run
    * Entire release
    * Entire application

---

## 🧠 Tech Stack

### Backend

* **Java 17**
* **Spring Boot**
* **Spring Web**
* **JDBC (SQLite / RDS)**
* **AWS SDK (S3)**

### Frontend

* HTML
* CSS
* Vanilla JavaScript
* Chart.js

### Infra

* Docker
* AWS ECR
* AWS S3
* (Optional) ECS / EC2

---

## 📂 Project Structure

```
.
├── src/main/java
│   ├── controller
│   │   ├── UploadController.java
│   │   ├── ChartController.java
│   │   └── AdminController.java
│   ├── service
│   │   └── ReportService.java
│   ├── repo
│   │   └── RunRepository.java
│   └── model
│       └── RunMeta.java
│
├── src/main/resources
│   ├── static/
│   │   ├── index.html
│   │   ├── app.js
│   │   └── style.css
│   └── application.yml
│
└── Dockerfile
```

---

## 🧪 API Endpoints

### Upload Report

```http
POST /api/upload
```

**Params:**

* `appId`
* `release`
* `executionDate`
* `file` (zip)

---

### Fetch Data

```http
GET /api/apps
GET /api/releases?appId=xyz
GET /api/runs?appId=xyz&release=1.0
```

---

### Analytics

```http
GET /api/charts/app?appId=app&from=2024-01-01&to=2024-01-31
GET /api/charts/release?appId=app&release=v1&from=2024-01-01&to=2024-01-31
```

---

### Delete APIs

```http
DELETE /api/admin/run/{runId}
DELETE /api/admin/release?appId=xyz&release=v1
DELETE /api/admin/app/{appId}
```

---

## 🐳 Docker Setup

```dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 7328
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

## ☁️ Deployment Options

| Method            | Recommended    |
| ----------------- | -------------- |
| Local Docker      | ✅              |
| EC2 + Docker      | ✅              |
| ECS + Fargate     | ⭐ Best         |
| Elastic Beanstalk | ✅              |
| Lambda            | ❌ Not suitable |

---

## 🌐 Access App

If running locally:

```
http://localhost:7328
```

If deployed on EC2/ECS:

```
http://<public-ip>:7328
```

With ALB:

```
https://<alb-dns-name>
```

---

## 🔐 Security Notes

* Add authentication if exposed publicly
* Use HTTPS via ALB
* Restrict S3 bucket access
* Avoid public write permissions

---

## 👨‍💻 Author

**Sidhartha Kar**
QA | Automation | DevOps Enthusiast

🔗 GitHub: [https://github.com/sidharthakar](https://github.com/sidharthakar)
🔗 LinkedIn: [https://linkedin.com/in/sidhartha-kar-sde](https://linkedin.com/in/sidhartha-kar-sde)

---

## ⭐ Support

If this project helped you, please ⭐ star the repository!

---

## 📌 Future Enhancements

* Role-based access
* OAuth login
* Multi-tenant support
* Test trend AI insights
* Slack / Email notifications

---

If you want, I can also:
✅ Add **architecture diagram**
✅ Convert this to **MkDocs / GitBook**
✅ Optimize for **GitHub SEO**
✅ Add **CI/CD pipeline yaml**

Just tell me 👍
