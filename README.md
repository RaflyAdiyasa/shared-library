
---

## 🏗️ Struktur Shared Library

```
shared-library/
├── src/com/course/
│   └── PipelineConfig.groovy      # Class model parser konfigurasi pipeline
├── vars/
│   ├── containerPipeline.groovy   # Entry point utama orchestrator CI pipeline
│   ├── buildAndPush.groovy        # Docker build (BuildKit) + injeksi build args + KinD load
│   ├── trivyScan.groovy           # DevSecOps scanning (filesystem & container image)
│   ├── updateGitops.groovy        # Otomatisasi update image tag di GitOps overlay
│   ├── sonarScan.groovy           # Scan code using sonarQube
│   ├── sonarQualityGate.groovy        # menunggu hasil scan sonarQube sebagain quality gate
│   └── notifySlack.groovy         # Notifikasi status build ke webhook Slack
└── examples/
    ├── Jenkinsfile                # Contoh inline script pipeline di Jenkins UI
    └── pipeline.yaml              # Contoh config declarative di repo aplikasi
```

---



## 🚀 Penggunaan di Pipeline Job

Cukup masukkan script berikut pada definisi Pipeline Job di Jenkins:

```groovy
@Library('course-shared-library') _

containerPipeline(
    appRepoUrl: 'https://github.com/RaflyAdiyasa/Helpdesk-Ticketing-API',
    appName: 'cocoa',
    language: 'go',
    testCommand: 'ls',
    imageName: 'huan271/cocoa',
    buildBranch: 'main',
    gitopsRepoUrl: 'https://github.com/RaflyAdiyasa/cocoa-gitops.git',
    gitopsPath: 'dev',
    gitopsDeployFile: 'deploy-be.yaml',
    sonarProjectKey: 'Learn',
    sonarqubeKeyID: 'sonarqube',
    sonarQualityGateTimeoutMinutes: '5',
    enableSecurityScan: true
)
```

---

