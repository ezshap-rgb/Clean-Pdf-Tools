# Clean PDF Tools Studio

A native, private, and powerful Android PDF Studio app built with Jetpack Compose, Kotlin, and Apache PDFBox.

![Clean PDF Tools](app/src/main/res/drawable/app_icon.png)

## Highlights

- **Streamlined, Dark Studio UI**: Matches the signature Clean Suite aesthetic (Clean Recorder, Clean Audio Trimmer) with deep violet background, metallic surface cards, electric violet and studio magenta gradients, and gold accents matching the 3D studio icon.
- **10 Core PDF Tools**:
  1. **Delete Pages**: Visual page selector with red trash indicators and range specification.
  2. **Merge PDFs**: Order-adjustable multi-document merger.
  3. **Reorder Pages**: Interactive page grid with left/right shift controls.
  4. **Extract Pages**: Multi-page selection to export custom sub-documents.
  5. **Split PDF**: Slider cutoff point splitting document into Part 1 and Part 2.
  6. **Rotate Pages**: 90° CW, 180° flip, and 90° CCW with live thumbnail rotation and scope options.
  7. **Images to PDF**: Convert photos and scanned images into a formatted PDF document.
  8. **PDF to Images**: High-resolution rendering to PNG or JPEG at 150 or 300 DPI.
  9. **Crop PDF**: Interactive visual guide overlay with draggable margin sliders for precision margin trimming.
  10. **Compress PDF**: Multi-level size optimization (Low, Medium Recommended, High).
- **Workflow Studio (Daisy-Chain Engine)**:
  - **Smart Recipes**:
    - **Replace Page**: Replace any page in a document with a single tap (internally executing split, delete, and merge).
    - **Insert in Middle**: Insert a document into the middle of an existing PDF at any page index.
  - **Custom Daisy-Chain Pipeline**: Construct multi-step sequential pipelines (e.g., Delete Pages -> Rotate -> Crop Margins -> Compress) with real-time step progress reporting.
- **Studio Library**:
  - History of processed PDFs, file size stats, page counts, quick viewing, system sharing, and direct export to Android Downloads.

## Standalone APK

The signed standalone APK is located at:
```
CleanPdfTools.apk
```

