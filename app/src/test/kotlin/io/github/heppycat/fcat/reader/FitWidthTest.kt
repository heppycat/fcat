package io.github.heppycat.fcat.reader

import io.github.heppycat.fcat.engine.PageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitWidthTest {
    private val a4 = PageSize(595f, 842f)

    @Test
    fun `page fills the viewport width and keeps its aspect ratio`() {
        val fit = fitWidth(a4, viewportWidthPx = 1080, maxPixels = Long.MAX_VALUE)
        assertEquals(1080 / 595f, fit.scale, 1e-6f)
        assertEquals(1080, fit.widthPx)
        assertEquals(1528, fit.heightPx) // 842 * 1080 / 595 = 1528.3
    }

    @Test
    fun `width never exceeds the viewport through float rounding`() {
        for (viewport in 1..3000) {
            val fit = fitWidth(PageSize(612.3f, 791.7f), viewport, Long.MAX_VALUE)
            assertEquals(viewport, fit.widthPx)
        }
    }

    @Test
    fun `a page too tall for the pixel budget is scaled down to fit it`() {
        val ribbon = PageSize(100f, 100_000f)
        val fit = fitWidth(ribbon, viewportWidthPx = 1000, maxPixels = 10_000_000)
        assertTrue(fit.widthPx < 1000)
        assertTrue(fit.widthPx.toLong() * fit.heightPx <= 10_000_000 * 1.001)
    }

    @Test
    fun `a tiny page still gets at least one pixel`() {
        val fit = fitWidth(PageSize(0.001f, 1000f), viewportWidthPx = 1, maxPixels = 1)
        assertTrue(fit.widthPx >= 1 && fit.heightPx >= 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty page is rejected`() {
        fitWidth(PageSize(0f, 842f), 1080, Long.MAX_VALUE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty viewport is rejected`() {
        fitWidth(a4, 0, Long.MAX_VALUE)
    }
}
