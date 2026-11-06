<div align="center">

<img src="docs/assets/banner.svg" alt="DODTH — Document Utility Suite" width="100%"/>

# DODTH — Document Utility Suite

**Every document, handled.** Convert, merge, compress, protect and sign — on the web and natively on Android, with privacy first.

[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)](https://react.dev)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev)
[![Firebase](https://img.shields.io/badge/Firebase-Hosting%20%2B%20Functions-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android_native-7F52FF?logo=kotlin&logoColor=white)](docs-android/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/license-proprietary-lightgrey)](#license)

</div>

---

## What is this?

DODTH is a professional document toolkit — one product, two experiences:

| | Web suite | Native Android app |
|---|---|---|
| Location | [`frontend/`](frontend/) + [`backend/`](backend/) | [`docs-android/`](docs-android/) |
| Stack | React 19 + Vite 8 · Express 5 on Firebase Functions (Node 20) | Kotlin + Jetpack Compose (Material 3), minSdk 24 |
| Processing | Client-side engines (pdf-lib, pdf.js, mammoth) + secured API | 100% on-device (PDFBox-Android, Android PdfDocument/PdfRenderer, ML Kit OCR) |
| Best for | Big jobs in the browser | Private, offline, mobile-first work |

## Tools

**Convert to PDF** — Word (.docx) → PDF · Excel (.xlsx) → PDF · PowerPoint (.pptx) → PDF · JPG/PNG/WebP/BMP → PDF · HTML → PDF

**Convert from PDF** — PDF → Word · PDF → Excel · PDF → PowerPoint · PDF → JPG · OCR → searchable PDF (Android)

**Organize** — Merge · Split · Rotate · Compress · Page numbers · Remove pages

**Security** — Protect (256-bit AES) · Unlock · Diagonal watermarking

## Architecture

```mermaid
flowchart LR
    U["You"] --> W["Web suite<br/>React + Vite"]
    U --> A["Android app<br/>Compose"]
    W -->|"/api/**"| F["Firebase Functions API<br/>Express 5 · sharp · pdf-lib · xlsx"]
    W -->|"direct"| C["Client-side engines<br/>pdf-lib · pdf.js · mammoth"]
    A -->|"fully on-device"| E["PDFBox-Android<br/>ML Kit OCR<br/>Android PdfDocument"]
    F --> S["In-memory streams only<br/>nothing persisted"]
    E --> D["Private on-device storage"]
```

## Quick start

**Web**

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # production bundle -> frontend/dist
```

**Backend (Firebase Functions)**

```bash
cd backend
npm install
npm run dev          # Express API with --watch
firebase deploy      # hosting (frontend/dist) + functions
```

**Android**

```bash
cd docs-android
gradle wrapper       # first time only (or open in Android Studio)
gradle :app:assembleDebug
```

Requires JDK 17 and Android SDK 35. Full notes: [docs-android/README.md](docs-android/README.md)

## Repository layout

```
├── frontend/          React 19 + Vite 8 web app (Firebase Hosting target)
│   └── capacitor.config.ts   Capacitor shell groundwork (Android)
├── backend/           Express 5 API on Firebase Functions (/api/** rewrites)
├── docs-android/      Native Android edition — 19 tools, offline-first
├── docs/assets/       Brand assets (animated banner)
└── firebase.json      Hosting (frontend/dist) + Functions configuration
```

## Privacy

- **Web** — files are processed in-memory: client-side engines, or API streams that are never written to disk. Uploads (`uploads/`, `outputs/`) are git-ignored.
- **Android** — nothing leaves the phone. No uploads, no accounts, no tracking, no ads.

## Roadmap

- [ ] Visible CI — APK build + web deploy via GitHub Actions
- [ ] Live Editor re-enable (currently hidden from production dashboard)
- [ ] iOS release via the existing Capacitor shell

## License

Proprietary — © 2026 DODTH. All rights reserved. (Backend tooling carries ISC for vendored dependencies.)
