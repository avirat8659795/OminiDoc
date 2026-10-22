# DocVault

Open **any** document, make PDFs from anything, and edit PDFs — entirely offline.

**No INTERNET permission.** Install it, turn on airplane mode, and everything still works.

## Features

- Opens PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, ODT/ODS/ODP, RTF, TXT, MD, CSV, JSON, XML,
  HTML, EPUB/MOBI, CBZ, images, SVG, archives, code files
- Full library management: folders, search, sort, grid/list, multi-select, rename,
  move, copy, delete, favourites, recents
- Scan to PDF from the camera
- Create PDFs from text and images
- Edit PDFs: pen, highlighter, text, shapes, signatures, colours, undo/redo
- Tools: merge, split, compress, password protect
- Registered as an "Open with" target for common document types
- Light / dark / system theme

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and Android SDK 35.

## Architecture

- Kotlin + Jetpack Compose, Material 3, custom design system
- Storage Access Framework — no storage permissions
- `android.graphics.pdf.PdfRenderer` for rendering
- PdfBox-Android for writing, merging, splitting, encrypting
- CameraX for scanning
- Coil + coil-pdf for thumbnails

## Licence

MIT