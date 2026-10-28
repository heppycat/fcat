// JNI glue for PdfiumEngine.kt. Deliberately thin: it maps PDFium's C API one call at a time and leaves
// logic (errors, text lines, outline walking) to Kotlin. PDFium is not thread-safe; the Kotlin side makes
// every call on one engine thread.

#include <android/bitmap.h>
#include <jni.h>
#include <sys/stat.h>
#include <unistd.h>

#include <cerrno>
#include <climits>
#include <cstdint>
#include <vector>

#include "fpdf_doc.h"
#include "fpdf_edit.h"
#include "fpdf_text.h"
#include "fpdfview.h"

namespace {

// FPDF_LoadCustomDocument keeps a pointer to the access struct, so it must live as long as the document.
struct Document {
    FPDF_FILEACCESS access{};
    int fd = -1;
    FPDF_DOCUMENT doc = nullptr;
};

// Handles cross JNI as jlong. Going through uintptr_t keeps 32-bit pointers positive, so negative values
// stay free for error codes.
template <typename T>
jlong toHandle(T* p) { return static_cast<jlong>(reinterpret_cast<uintptr_t>(p)); }

template <typename T>
T fromHandle(jlong h) { return reinterpret_cast<T>(static_cast<uintptr_t>(h)); }

FPDF_DOCUMENT docOf(jlong h) { return fromHandle<Document*>(h)->doc; }

int readBlock(void* param, unsigned long position, unsigned char* buf, unsigned long size) {
    int fd = static_cast<Document*>(param)->fd;
    while (size > 0) {
        ssize_t n = pread64(fd, buf, size, static_cast<off64_t>(position));
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) return 0;
        buf += n;
        position += n;
        size -= n;
    }
    return 1;
}

}  // namespace

#define JNI_FN(name) JNIEXPORT JNICALL Java_io_github_heppycat_fcat_engine_PdfiumNative_##name

extern "C" {

void JNI_FN(init)(JNIEnv*, jclass) {
    // Non-embedded fonts other than the standard 14 are looked up among the system fonts.
    static const char* fontPaths[] = {"/system/fonts", nullptr};
    FPDF_LIBRARY_CONFIG config{};
    config.version = 2;
    config.m_pUserFontPaths = fontPaths;
    FPDF_InitLibraryWithConfig(&config);
}

// Returns a document handle, or minus an FPDF_ERR_* code.
jlong JNI_FN(open)(JNIEnv* env, jclass, jint fd, jstring password) {
    struct stat64 st{};
    if (fstat64(fd, &st) != 0) return -FPDF_ERR_FILE;
    // m_FileLen is an unsigned long: 32 bits on 32-bit ABIs.
    if (static_cast<unsigned long long>(st.st_size) > ULONG_MAX) return -FPDF_ERR_FILE;

    auto* d = new Document;
    d->fd = fd;
    d->access.m_FileLen = static_cast<unsigned long>(st.st_size);
    d->access.m_GetBlock = readBlock;
    d->access.m_Param = d;

    const char* pw = password ? env->GetStringUTFChars(password, nullptr) : nullptr;
    d->doc = FPDF_LoadCustomDocument(&d->access, pw);
    if (pw) env->ReleaseStringUTFChars(password, pw);

    if (!d->doc) {
        unsigned long err = FPDF_GetLastError();
        delete d;
        return -static_cast<jlong>(err == FPDF_ERR_SUCCESS ? FPDF_ERR_UNKNOWN : err);
    }
    return toHandle(d);
}

void JNI_FN(close)(JNIEnv*, jclass, jlong doc) {
    auto* d = fromHandle<Document*>(doc);
    FPDF_CloseDocument(d->doc);
    delete d;
}

jint JNI_FN(pageCount)(JNIEnv*, jclass, jlong doc) { return FPDF_GetPageCount(docOf(doc)); }

jboolean JNI_FN(pageSize)(JNIEnv* env, jclass, jlong doc, jint index, jfloatArray out) {
    FS_SIZEF size;
    if (!FPDF_GetPageSizeByIndexF(docOf(doc), index, &size)) return JNI_FALSE;
    jfloat values[] = {size.width, size.height};
    env->SetFloatArrayRegion(out, 0, 2, values);
    return JNI_TRUE;
}

jlong JNI_FN(loadPage)(JNIEnv*, jclass, jlong doc, jint index) { return toHandle(FPDF_LoadPage(docOf(doc), index)); }

void JNI_FN(closePage)(JNIEnv*, jclass, jlong page) { FPDF_ClosePage(fromHandle<FPDF_PAGE>(page)); }

// Writes left, bottom, right, top of the visible page box in PDF user space, then the rotation in quarter turns.
jboolean JNI_FN(pageFrame)(JNIEnv* env, jclass, jlong page, jfloatArray out) {
    auto p = fromHandle<FPDF_PAGE>(page);
    FS_RECTF box;
    int rotation = FPDFPage_GetRotation(p);
    if (!FPDF_GetPageBoundingBox(p, &box) || rotation < 0) return JNI_FALSE;
    jfloat values[] = {box.left, box.bottom, box.right, box.top, static_cast<jfloat>(rotation)};
    env->SetFloatArrayRegion(out, 0, 5, values);
    return JNI_TRUE;
}

// Draws the page, scaled to pageWidth x pageHeight pixels, so that page pixel (left, top) lands on the
// target's (0, 0). Only the target's top-left width x height pixels are touched.
jboolean JNI_FN(render)(JNIEnv* env, jclass, jlong page, jobject target, jint left, jint top, jint width,
                        jint height, jint pageWidth, jint pageHeight) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, target, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || static_cast<uint32_t>(width) > info.width ||
        static_cast<uint32_t>(height) > info.height) {
        return JNI_FALSE;
    }
    void* pixels;
    if (AndroidBitmap_lockPixels(env, target, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) return JNI_FALSE;

    FPDF_BITMAP view = FPDFBitmap_CreateEx(width, height, FPDFBitmap_BGRA, pixels, static_cast<int>(info.stride));
    if (view) {
        FPDFBitmap_FillRect(view, 0, 0, width, height, 0xFFFFFFFF);
        // ARGB_8888 is laid out R, G, B, A in memory: BGRA with the byte order reversed.
        FPDF_RenderPageBitmap(view, fromHandle<FPDF_PAGE>(page), -left, -top, pageWidth, pageHeight, 0,
                              FPDF_ANNOT | FPDF_REVERSE_BYTE_ORDER);
        FPDFBitmap_Destroy(view);
    }
    AndroidBitmap_unlockPixels(env, target);
    return view ? JNI_TRUE : JNI_FALSE;
}

// Six floats per character: code point (0 if unknown), 1 if PDFium generated it (spaces, line breaks),
// then left, bottom, right, top in PDF user space. Null if the text cannot be loaded.
jfloatArray JNI_FN(pageChars)(JNIEnv* env, jclass, jlong page) {
    FPDF_TEXTPAGE text = FPDFText_LoadPage(fromHandle<FPDF_PAGE>(page));
    if (!text) return nullptr;
    int count = FPDFText_CountChars(text);
    std::vector<jfloat> values(count > 0 ? static_cast<size_t>(count) * 6 : 0);
    for (int i = 0; i < count; i++) {
        // The loose box spans the glyph's advance and the font's ascent and descent, like MuPDF's, rather than
        // the inked shape that FPDFText_GetCharBox returns.
        FS_RECTF box{};
        FPDFText_GetLooseCharBox(text, i, &box);
        jfloat* v = &values[static_cast<size_t>(i) * 6];
        v[0] = static_cast<jfloat>(FPDFText_GetUnicode(text, i));
        v[1] = FPDFText_IsGenerated(text, i) == 1 ? 1.f : 0.f;
        v[2] = box.left;
        v[3] = box.bottom;
        v[4] = box.right;
        v[5] = box.top;
    }
    FPDFText_ClosePage(text);
    jfloatArray out = env->NewFloatArray(static_cast<jsize>(values.size()));
    if (out) env->SetFloatArrayRegion(out, 0, static_cast<jsize>(values.size()), values.data());
    return out;
}

// parent 0 means the outline root. Returns 0 when there is no child.
jlong JNI_FN(bookmarkFirstChild)(JNIEnv*, jclass, jlong doc, jlong parent) {
    return toHandle(FPDFBookmark_GetFirstChild(docOf(doc), fromHandle<FPDF_BOOKMARK>(parent)));
}

jlong JNI_FN(bookmarkNextSibling)(JNIEnv*, jclass, jlong doc, jlong bookmark) {
    return toHandle(FPDFBookmark_GetNextSibling(docOf(doc), fromHandle<FPDF_BOOKMARK>(bookmark)));
}

jstring JNI_FN(bookmarkTitle)(JNIEnv* env, jclass, jlong bookmark) {
    auto bm = fromHandle<FPDF_BOOKMARK>(bookmark);
    unsigned long bytes = FPDFBookmark_GetTitle(bm, nullptr, 0);
    if (bytes <= 2) return env->NewStringUTF("");
    std::vector<jchar> title(bytes / 2);
    FPDFBookmark_GetTitle(bm, title.data(), bytes);
    return env->NewString(title.data(), static_cast<jsize>(title.size() - 1));  // Drop the terminating NUL.
}

// The target page of a bookmark, from its destination or its GoTo action; -1 for anything else.
jint JNI_FN(bookmarkPageIndex)(JNIEnv*, jclass, jlong doc, jlong bookmark) {
    FPDF_DOCUMENT d = docOf(doc);
    auto bm = fromHandle<FPDF_BOOKMARK>(bookmark);
    FPDF_DEST dest = FPDFBookmark_GetDest(d, bm);
    if (!dest) {
        FPDF_ACTION action = FPDFBookmark_GetAction(bm);
        if (action && FPDFAction_GetType(action) == PDFACTION_GOTO) dest = FPDFAction_GetDest(d, action);
    }
    return dest ? FPDFDest_GetDestPageIndex(d, dest) : -1;
}

}  // extern "C"
