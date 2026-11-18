package ec.edu.uleam.bookshelf.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BooksHttpTest {
    private val server = MockWebServer()
    private lateinit var repository: GoogleBooksRepository

    @Before fun setup() {
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/books/v1/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(BooksApi::class.java)
        repository = GoogleBooksRepository(api)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun parsesRealHttpResponsesThroughRetrofitAndGson() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":[{"id":"abc"}]}"""))
        server.enqueue(MockResponse().setBody("""{"volumeInfo":{"title":"Prueba HTTP","imageLinks":{"thumbnail":"http://example.com/cover"}}}"""))
        val book = repository.search("novela histórica").single()
        assertEquals("Prueba HTTP", book.title)
        assertEquals("https://example.com/cover", book.cover)
        val search = server.takeRequest().requestUrl!!
        assertEquals("/books/v1/volumes", search.encodedPath)
        assertEquals("novela histórica", search.queryParameter("q"))
        assertEquals("/books/v1/volumes/abc", server.takeRequest().path)
    }

    @Test fun skipsDeletedVolumeAndContinues() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":[{"id":"gone"},{"id":"ok"}]}"""))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setBody("""{"volumeInfo":{"title":"Disponible"}}"""))
        assertEquals("Disponible", repository.search("texto").single().title)
    }

    @Test fun propagatesQuotaError() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        try {
            repository.search("libros")
            fail("Se esperaba un error de cuota")
        } catch (e: HttpException) {
            assertEquals(429, e.code())
        }
    }
}
