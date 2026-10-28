package io.github.heppycat.fcat.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What every [PdfEngine] must do, run against the fixtures that make_fixtures.py writes (their geometry is
 * documented there). Each engine gets a subclass.
 */
abstract class PdfEngineContractTest {
    protected abstract val engine: PdfEngine
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun asset(name: String): ByteArray = instrumentation.context.assets.open(name).use { it.readBytes() }

    private fun descriptorOf(bytes: ByteArray): ParcelFileDescriptor {
        val file = File.createTempFile("fixture", ".pdf", instrumentation.targetContext.cacheDir)
        try {
            file.writeBytes(bytes)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } finally {
            file.delete() // The open descriptor keeps the data readable.
        }
    }

    private fun withFixture(name: String = "fixture.pdf", password: String? = null, block: (PdfDocument) -> Unit) =
        runBlocking(engine.dispatcher) { engine.open(descriptorOf(asset(name)), password).use(block) }

    @Test
    fun readsPageCountAndRotatedSizes() = withFixture { doc ->
        assertEquals(2, doc.pageCount)
        assertEquals(PageSize(595f, 842f), doc.pageSize(0))
        assertEquals(PageSize(842f, 595f), doc.pageSize(1))
    }

    @Test
    fun readsNestedOutlineWithPageTargets() = withFixture { doc ->
        assertEquals(
            listOf(
                OutlineItem("Chapter 1", 0, listOf(OutlineItem("Section 1.1", 1, emptyList()))),
                OutlineItem("Website", -1, emptyList()),
            ),
            doc.outline(),
        )
    }

    @Test
    fun extractsTextWithPositionsFromTopLeft() = withFixture { doc ->
        val line = doc.pageText(0).lines.single { it.text == "Hello fcat" }
        // Baseline at 72 pt from the top, 24 pt Helvetica, starting at x = 72.
        assertEquals(72f, line.left, 1f)
        assertTrue("top ${line.top}", line.top in 40f..72f)
        assertTrue("bottom ${line.bottom}", line.bottom in 72f..85f)
        assertEquals(listOf("Second page"), doc.pageText(1).lines.map { it.text })
    }

    @Test
    fun rendersWholePageAtScaleOne() = withFixture { doc ->
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        doc.renderRegion(0, Rect(0, 0, 595, 842), 1f, bitmap)
        // Square covers 100..200 in both axes.
        assertEquals(Color.BLACK, bitmap.getPixel(150, 150))
        assertEquals(Color.BLACK, bitmap.getPixel(101, 198))
        assertEquals(Color.WHITE, bitmap.getPixel(98, 150))
        assertEquals(Color.WHITE, bitmap.getPixel(150, 202))
        assertEquals(Color.WHITE, bitmap.getPixel(500, 800))
    }

    @Test
    fun rendersOnlyTheRegionIntoTheTopLeftOfTheTarget() = withFixture { doc ->
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        // At scale 2 the square covers 200..400, so this region straddles its top-left corner.
        doc.renderRegion(0, Rect(180, 180, 220, 220), 2f, bitmap)
        assertEquals(Color.WHITE, bitmap.getPixel(10, 10)) // page pixel (190, 190)
        assertEquals(Color.BLACK, bitmap.getPixel(30, 30)) // page pixel (210, 210)
        assertEquals(Color.RED, bitmap.getPixel(50, 10)) // outside the 40 x 40 region: untouched
        assertEquals(Color.RED, bitmap.getPixel(10, 50))
    }

    @Test
    fun rejectsPageIndexOutOfRange() = withFixture { doc ->
        assertThrows(IndexOutOfBoundsException::class.java) { doc.pageSize(2) }
        assertThrows(IndexOutOfBoundsException::class.java) { doc.pageText(-1) }
        Unit
    }

    @Test
    fun encryptedDocumentNeedsTheRightPassword() {
        runBlocking(engine.dispatcher) {
            val bytes = asset("encrypted.pdf")
            assertThrows(PdfPasswordException::class.java) { engine.open(descriptorOf(bytes)) }
            assertThrows(PdfPasswordException::class.java) { engine.open(descriptorOf(bytes), "wrong") }
        }
        withFixture("encrypted.pdf", password = "fcat") { doc ->
            assertEquals(2, doc.pageCount)
            assertTrue(doc.pageText(0).lines.any { it.text == "Hello fcat" })
        }
    }

    @Test
    fun notAPdfFailsAndClosesTheDescriptor() = runBlocking(engine.dispatcher) {
        val file = descriptorOf("this is not a PDF\n".repeat(100).toByteArray())
        val error = assertThrows(Exception::class.java) { engine.open(file) }
        assertFalse(error is PdfPasswordException)
        assertFalse(file.fileDescriptor.valid())
    }

    @Test
    fun truncatedPdfOpensRepairedOrFailsCleanly() = runBlocking(engine.dispatcher) {
        val bytes = asset("fixture.pdf")
        // Either outcome is acceptable; a native crash or a hang is not.
        val doc = runCatching { engine.open(descriptorOf(bytes.copyOf(bytes.size / 2))) }.getOrNull()
        doc?.use {
            if (it.pageCount > 0) {
                runCatching {
                    it.renderRegion(0, Rect(0, 0, 10, 10), 1f, Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
                }
            }
        }
        Unit
    }
}
