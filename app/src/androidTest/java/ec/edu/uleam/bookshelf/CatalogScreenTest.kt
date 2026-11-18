package ec.edu.uleam.bookshelf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.EventListener
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ec.edu.uleam.bookshelf.data.BooksApi
import ec.edu.uleam.bookshelf.data.GoogleBooksRepository
import ec.edu.uleam.bookshelf.ui.BookshelfScreen
import ec.edu.uleam.bookshelf.ui.ShelfViewModel
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

class CatalogScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchLoadsBothTitlesAndCoverImages() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("127.0.0.1").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).build()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory(), false)
            server.start()
            val baseUrl = server.url("/").newBuilder().host("127.0.0.1").build()
            val books = mapOf("mar" to "Cartas del litoral", "ruta" to "Rutas de Manabí")
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.requestUrl!!.encodedPath
                    val id = path.substringAfterLast('/')
                    return when {
                        path == "/volumes" -> MockResponse().setBody("""{"items":[{"id":"mar"},{"id":"ruta"}]}""")
                        path.startsWith("/volumes/") && id in books -> {
                            val cover = baseUrl.resolve("/covers/$id")!!
                            val author = if (id == "mar") "Ana Vera" else "Luis Cedeño"
                            MockResponse().setBody("""{"volumeInfo":{"title":"${books[id]}","authors":["$author"],"imageLinks":{"thumbnail":"$cover"}}}""")
                        }
                        path.startsWith("/covers/") && id in books -> {
                            MockResponse().setHeader("Content-Type", "image/png")
                                .setBody(coverImage(id, books.getValue(id)))
                        }
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
            val coversLoaded = ConcurrentHashMap.newKeySet<String>()
            val imageLoader = ImageLoader.Builder(context).okHttpClient(client)
                .networkObserverEnabled(false)
                .eventListener(object : EventListener {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                        coversLoaded.add(request.data.toString())
                    }
                }).build()
            Coil.setImageLoader(imageLoader)
            try {
                val api = Retrofit.Builder().baseUrl(baseUrl).client(client)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(BooksApi::class.java)
                compose.setContent {
                    val model: ShelfViewModel = viewModel { ShelfViewModel(GoogleBooksRepository(api)) }
                    val state by model.state.collectAsStateWithLifecycle()
                    BookshelfScreen(state, model.lastQuery, model::search, model::browseLocal)
                }
                compose.waitUntil(timeoutMillis = 90_000) { coversLoaded.size == books.size }
                compose.onNodeWithText("Cartas del litoral").assertIsDisplayed()
                compose.onNodeWithText("Rutas de Manabí").assertIsDisplayed()
                compose.waitForIdle()
                if (InstrumentationRegistry.getArguments().getString("capture") == "true") {
                    val bitmap = instrumentation.uiAutomation.takeScreenshot()
                    context.openFileOutput("catalog.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            } finally {
                imageLoader.shutdown()
                Coil.reset()
            }
        }
    }

    private fun coverImage(id: String, title: String): Buffer {
        val bitmap = Bitmap.createBitmap(240, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val coast = id == "mar"
        canvas.drawColor(Color.parseColor(if (coast) "#235956" else "#993E2B"))
        paint.color = Color.parseColor("#ECCB92")
        if (coast) {
            canvas.drawCircle(172f, 124f, 29f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            repeat(7) { i -> canvas.drawArc(-50f, 152f + i*10, 315f, 233f + i*10, 190f, 152f, false, paint) }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            repeat(6) { i ->
                canvas.drawCircle(137f, 177f, 12f + i * 14f, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = Color.WHITE
        canvas.drawText(title, 15f, 42f, paint)
        val buffer = Buffer()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer.outputStream())
        bitmap.recycle()
        return buffer
    }
}
