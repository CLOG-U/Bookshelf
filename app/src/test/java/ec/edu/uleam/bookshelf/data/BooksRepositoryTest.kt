package ec.edu.uleam.bookshelf.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private class FakeBooksApi(
    private val ids: List<VolumeRef>?,
    private val details: Map<String, Volume> = emptyMap(),
) : BooksApi {
    val requestedIds = mutableListOf<String>()
    var term = ""
    override suspend fun search(query: String, apiKey: String?, limit: Int, fields: String): SearchResponse {
        term = query
        return SearchResponse(ids)
    }
    override suspend fun volume(id: String, apiKey: String?): Volume {
        requestedIds += id
        return details.getValue(id)
    }
}

class BooksRepositoryTest {
    @Test fun mapsDetailsInOrderAndUsesHttps() = runTest {
        val api = FakeBooksApi(listOf(VolumeRef("one"), VolumeRef("two")), mapOf(
            "one" to Volume(VolumeInfo("Primero", listOf("Ana", "Luis"), ImageLinks("http://example.com/1", null))),
            "two" to Volume(VolumeInfo("Segundo", null, ImageLinks(null, "https://example.com/2"))),
        ))
        val books = GoogleBooksRepository(api).search("  historia  ")
        assertEquals("historia", api.term)
        assertEquals(listOf("one", "two"), api.requestedIds)
        assertEquals(listOf(
            Book("one", "Primero", "Ana, Luis", "https://example.com/1"),
            Book("two", "Segundo", "", "https://example.com/2"),
        ), books)
    }

    @Test fun emptyResponseDoesNotRequestDetails() = runTest {
        val api = FakeBooksApi(null)
        assertTrue(GoogleBooksRepository(api).search("sin coincidencias").isEmpty())
        assertTrue(api.requestedIds.isEmpty())
    }

    @Test fun ignoresMissingIdsDuplicatesAndMissingTitles() = runTest {
        val api = FakeBooksApi(listOf(VolumeRef(null), VolumeRef(""), VolumeRef("a"),
            VolumeRef("b"), VolumeRef("a")), mapOf(
            "a" to Volume(VolumeInfo("Libro sin portada", null, null)),
            "b" to Volume(VolumeInfo(null, null, null)),
        ))
        val books = GoogleBooksRepository(api).search("libros")
        assertEquals(listOf("a", "b"), api.requestedIds)
        assertEquals(Book("a", "Libro sin portada", "", null), books.single())
    }
}
