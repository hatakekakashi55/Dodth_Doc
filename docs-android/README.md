# Docs — Document Utility Suite (Native Android)

<p>
  <strong>Docs</strong> is the native Android edition of the Docs document suite.
  It is a fully self-contained, on-device document toolkit — every feature runs
  inside the app. No backend, no uploads, no accounts, no ads, no emojis.
</p>

## Why this app exists

The web suite (`../frontend`) talks to a hosted processing backend. This Android
edition reimplements the entire pipeline natively so your files never leave the
phone: parsing, rendering, encryption and OCR all happen locally.

## Features (19 tools, 4 categories)

| Category | Tools |
|---|---|
| Convert to PDF | Word to PDF · Excel to PDF · PPT to PDF · JPG/PNG/WebP/BMP to PDF · HTML to PDF |
| Convert from PDF | PDF to Word (.docx) · PDF to PPT (.pptx) · PDF to Excel (.xlsx) · PDF to JPG (zipped) · OCR PDF (searchable) |
| Organize PDF | Merge · Split · Rotate (90/180/270°) · Compress · Page numbers · Remove pages |
| PDF Security | Protect (256-bit AES) · Unlock · Diagonal text watermark |

Plus:

- **Recents** — every output is kept privately on-device (last 50), ready to reopen, share or re-save
- **Save to Downloads/Docs** — one tap, via MediaStore
- **Share / Open** — Android share sheet + registered viewers via FileProvider
- **Live search** across the tool catalogue
- **Appearance** — System / Light / Dark, persisted across launches
- **Brand identity** — custom launcher icons, gradient hero banner, enterprise slate & indigo design system

## Architecture

```
com.docs.app
├── DocsApplication          # PDFBox init, theme prefs
├── MainActivity             # edge-to-edge host, bottom navigation
├── core/                    # Files/IO helpers, ZIP authoring, Task→coroutine bridge
├── data/                    # Tool catalogue, history store (org.json index), theme prefs
├── domain/
│   ├── ProcessingDispatcher # tool-id → engine routing, output naming, summaries
│   ├── pdf/                 # merge/split/rotate/compress/page-numbers/delete (PDFBox),
│   │                        # protect/unlock/watermark, text extraction, PDF→JPG (PdfRenderer)
│   ├── convert/             # Word→PDF, Excel→PDF, PPT→PDF, HTML→PDF,
│   │                        # PDF→Word/Excel/PPT (text + hand-rolled OOXML writers)
│   ├── ooxml/               # .docx/.xlsx readers + writers, .pptx reader + writer, XML escaping
│   ├── render/              # Flow model, paginated PDF composer (StaticLayout), table renderer
│   └── ocr/                 # ML Kit pipeline → searchable PDF (image + invisible text layer)
└── ui/
    ├── theme/               # Color / Type / Shapes / DocsTheme (Material 3)
    ├── navigation/          # NavHost + route table
    ├── home/                # Dashboard, hero banner, tool cards, search
    ├── tool/                # Universal tool workspace (StateFlow + coroutine ticker)
    ├── history/             # Recent outputs
    ├── settings/            # Appearance, storage, licenses
    └── components/          # FileExtBadge, EmptyState, dividers
```

### Engine choices (all offline, all on-device)

| Need | Engine |
|---|---|
| PDF merge / split / rotate / compress / delete / page numbers / encrypt / watermark / text | [Apache PDFBox for Android](https://github.com/TomRoush/PdfBox-Android) (`com.tom-roush:pdfbox-android`) |
| Images → PDF, HTML → PDF, Office → PDF layout | Android framework `PdfDocument` + `StaticLayout` |
| PDF → JPG | Android framework `PdfRenderer` |
| OCR | Google ML Kit Text Recognition (Latin, on-device model) |
| .docx / .xlsx / .pptx read & write | Hand-rolled, minimal-but-valid OOXML (no server, no POI) |

### Honest limitations

- `.doc` / `.xls` / `.ppt` (legacy binary OLE formats) cannot be parsed — convert them to the OpenXML variant first (Word/Excel/PowerPoint can do this). The app will tell you if a file is not readable.
- PDF → Office conversions extract *text* (layout is rebuilt as clean, generic documents), matching and slightly exceeding the fidelity of text-based server conversions. Image-only/scanned PDFs yield a clear "run OCR first" message instead of a broken file.
- Compression rewrites and de-duplicates PDF objects (same technique as the web pipeline); it does not re-encode embedded images.
- The OCR model (~4 MB) downloads once through Play services on first use; after that OCR works fully offline.

## Building

Requirements: Android Studio Ladybug (2024.2.1) or newer, JDK 17, Android SDK 35.

1. Open the `docs-android/` folder in Android Studio → *Open* (not Import).
2. Let Gradle sync (dependencies resolve from `google()` and `mavenCentral()`).
3. Run on any device/emulator with **Android 7.0+ (API 24)**.

Command line (with a Gradle 8.9 wrapper or local Gradle):

```bash
gradle :app:assembleDebug          # debug APK
gradle :app:assembleRelease        # minified release APK (R8)
gradle :app:installDebug           # build + install to connected device
```

> The repository does not carry a `gradle-wrapper.jar`; running `gradle wrapper`
> once in this folder regenerates it, or just use Android Studio's bundled Gradle.

### Application identity

- **Name:** `Docs` (`@string/app_name`)
- **applicationId / namespace:** `com.docs.app`
- **Version:** 1.0.0 (versionCode 1)
- **Min SDK:** 24 · **Target / Compile SDK:** 35

## Design language

Enterprise slate & indigo — the same DNA as the web suite:

- Background `#1A1C22`, surfaces `#242731`, accent indigo `#6366F1 → #818CF8`
- Success `#10B981`, danger `#EF4444`, warning `#F59E0B`
- Material 3, tonal elevation, hairline outline strokes, 20–22dp corner radius on hero surfaces
- Professional iconography only (Material symbols); zero emojis anywhere in the UI
