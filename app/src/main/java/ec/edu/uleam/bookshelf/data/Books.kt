package ec.edu.uleam.bookshelf.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Book(val id: String, val title: String, val author: String, val cover: String?)
data class SearchResponse(val items: List<VolumeRef>?)
data class VolumeRef(val id: String?)
data class Volume(val volumeInfo: VolumeInfo?)
data class VolumeInfo(val title: String?, val authors: List<String>?, val imageLinks: ImageLinks?)
data class ImageLinks(val thumbnail: String?, val smallThumbnail: String?)

interface BooksApi {
    @GET("volumes")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String?,
        @Query("maxResults") limit: Int = 12,
        @Query("fields") fields: String = "items(id)",
    ): SearchResponse

    @GET("volumes/{id}")
    suspend fun volume(@Path("id") id: String, @Query("key") apiKey: String?): Volume
}

interface BooksRepository {
    suspend fun search(query: String): List<Book>
}

class GoogleBooksRepository(private val api: BooksApi, private val key: String? = null) : BooksRepository {
    override suspend fun search(query: String): List<Book> {
        val ids = api.search(query.trim(), key).items.orEmpty()
            .mapNotNull { it.id?.takeIf(String::isNotBlank) }.distinct()
        return ids.mapNotNull { id ->
            val info = try {
                api.volume(id, key).volumeInfo
            } catch (error: retrofit2.HttpException) {
                if (error.code() == 404) null else throw error
            } ?: return@mapNotNull null
            val title = info.title?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val url = info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail
            Book(id, title, info.authors.orEmpty().joinToString(", "),
                url?.replaceFirst("http://", "https://")?.takeIf { it.startsWith("https://") })
        }
    }
}

val localClassics = listOf(
    Book("quijote", "Don Quijote de la Mancha", "Miguel de Cervantes", null),
    Book("frankenstein", "Frankenstein", "Mary Shelley", null),
    Book("vuelta", "La vuelta al mundo en ochenta días", "Julio Verne", null),
    Book("orgullo", "Orgullo y prejuicio", "Jane Austen", null),
)
