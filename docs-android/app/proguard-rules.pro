# Docs — release rules (R8)
# PDFBox is reflection-light but keep enums and model classes intact.
-keep class org.apache.pdfbox.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.pdfbox.**
# BouncyCastle (pulled in by pdfbox-android; required for AES encryption)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
# ML Kit text recognition resolves its model via name lookups
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
# Keep coroutine debug metadata happy in release
-dontwarn kotlinx.coroutines.**
