# Project-specific R8 rules. Add rules here only with a comment explaining why.

# MuPDF's JNI layer looks up its Java classes, fields and methods by name at load time
# (find_fids in platform/java/mupdf_native.c). The AAR ships no consumer rules.
-keep class com.artifex.mupdf.fitz.** { *; }
