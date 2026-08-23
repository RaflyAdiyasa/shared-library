# Jenkins Shared Library — `course-shared-library`

Repository ini berisi **Global Trusted Pipeline Library** standar enterprise untuk mengotomatiskan build, security scan, docker packaging, dan update GitOps repository secara tersentralisasi.

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
│   └── notifySlack.groovy         # Notifikasi status build ke webhook Slack
└── examples/
    ├── Jenkinsfile                # Contoh inline script pipeline di Jenkins UI
    └── pipeline.yaml              # Contoh config declarative di repo aplikasi
```

---

## ⚙️ Cara Registrasi di Jenkins UI

1. Buka **Dashboard** ➔ **Manage Jenkins** ➔ **System**.
2. Scroll ke bagian **Global Pipeline Libraries**, klik **Add**:
   - **Name**: `course-shared-library`
   - **Default version**: `main`
   - **Retrieval method**: *Modern SCM* ➔ *Git*
   - **Project Repository**: `https://github.com/USERNAME/shared-library.git`
   - **Credentials**: `github-jenkins-token`
3. Klik **Save**.

---

## 🚀 Penggunaan di Pipeline Job

Cukup masukkan script berikut pada definisi Pipeline Job di Jenkins:

```groovy
@Library('course-shared-library') _

containerPipeline(
    appRepoUrl: 'https://github.com/USERNAME/backend-go.git',
    appName: 'backend-go',
    buildBranch: 'development',
    imageRegistry: 'backend-go',
    gitopsRepoUrl: 'https://github.com/USERNAME/gitops.git',
    gitopsBranch: 'main',
    gitopsOverlayPath: 'applications/development/backend-go',
    enableSecurityScan: false // Set true untuk mengaktifkan Trivy scan
)
```

---

## 🧩 Metadata Build & Injeksi Otomatis

Pada tahap `buildAndPush.groovy`, script secara otomatis mengambil metadata Git dan menginjeksikannya ke Docker build arg:
- `BUILD_NUMBER`: Nomor build Jenkins (sequential, traceable)
- `COMMIT_SHA`: Git short commit hash
- `COMMIT_MESSAGE`: Teks pesan commit Git terbaru

Variabel ini tertanam permanen di binary aplikasi (via ldflags) dan dapat dicek pada endpoint `/healthz`.
