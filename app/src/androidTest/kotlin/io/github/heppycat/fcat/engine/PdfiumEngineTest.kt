package io.github.heppycat.fcat.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfiumEngineTest : PdfEngineContractTest() {
    override val engine: PdfEngine = PdfiumEngine
}
