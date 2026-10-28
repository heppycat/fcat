package io.github.heppycat.fcat.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.heppycat.fcat.R
import io.github.heppycat.fcat.engine.PdfPasswordException
import io.github.heppycat.fcat.reader.renderFirstPage
import io.github.heppycat.fcat.ui.theme.FcatTheme
import kotlinx.coroutines.CancellationException
import java.io.FileNotFoundException

private const val TAG = "FirstPageScreen"

/** M1a: pick a PDF with the system picker and show its first page fitted to the width. No scroll or zoom. */
@Composable
fun FirstPageScreen() {
    var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) uri = picked
    }
    val openPdf = { picker.launch(arrayOf("application/pdf")) }

    Scaffold { padding ->
        val current = uri
        if (current == null) {
            EmptyState(openPdf, Modifier.fillMaxSize().padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Button(onClick = openPdf, modifier = Modifier.padding(8.dp)) {
                    Text(stringResource(R.string.open_pdf))
                }
                FirstPage(current, Modifier.fillMaxSize())
            }
        }
    }
}

private sealed interface PageState {
    data object Loading : PageState
    class Shown(val page: ImageBitmap) : PageState
    class Failed(@param:StringRes val message: Int) : PageState
}

@Composable
private fun FirstPage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth
        // Up to four screens of pixels: enough for any normal page at full width (see fitWidth).
        val maxPixels = widthPx.toLong() * constraints.maxHeight * 4
        val state by produceState<PageState>(PageState.Loading, uri, widthPx) {
            value = PageState.Loading
            value = try {
                val openFile = {
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw FileNotFoundException("Provider returned no descriptor")
                }
                PageState.Shown(renderFirstPage(openFile, widthPx, maxPixels).asImageBitmap())
            } catch (e: CancellationException) {
                throw e
            } catch (e: PdfPasswordException) {
                PageState.Failed(R.string.error_password)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open document", e)
                PageState.Failed(R.string.error_open)
            }
        }
        when (val s = state) {
            PageState.Loading -> {
                val description = stringResource(R.string.loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            }
            is PageState.Shown -> Image(
                bitmap = s.page,
                contentDescription = stringResource(R.string.first_page_description),
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
            )
            is PageState.Failed -> Text(
                stringResource(s.message),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun EmptyState(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpen) { Text(stringResource(R.string.open_pdf)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    FcatTheme { Box { EmptyState(onOpen = {}) } }
}
