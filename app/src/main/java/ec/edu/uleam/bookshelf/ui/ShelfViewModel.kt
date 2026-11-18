package ec.edu.uleam.bookshelf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uleam.bookshelf.data.Book
import ec.edu.uleam.bookshelf.data.BooksRepository
import ec.edu.uleam.bookshelf.data.localClassics
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.JsonParseException

sealed interface ShelfState {
    data object Loading : ShelfState
    data class Results(val books: List<Book>, val local: Boolean = false) : ShelfState
    data class Error(val message: String) : ShelfState
}

class ShelfViewModel(private val repository: BooksRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<ShelfState>(ShelfState.Loading)
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    var lastQuery = "literatura"
        private set

    init { search(lastQuery) }

    fun search(query: String) {
        val term = query.trim()
        if (term.isEmpty()) return
        lastQuery = term
        request?.cancel()
        mutableState.value = ShelfState.Loading
        request = viewModelScope.launch {
            mutableState.value = try {
                ShelfState.Results(repository.search(term))
            } catch (e: HttpException) {
                ShelfState.Error(when (e.code()) {
                    403, 429 -> "Google Books ha limitado las consultas. Puedes volver a intentar más tarde."
                    else -> "Google Books no pudo completar la consulta."
                })
            } catch (e: IOException) {
                ShelfState.Error("Revisa tu conexión a internet y vuelve a intentar.")
            } catch (e: JsonParseException) {
                ShelfState.Error("Google Books envió una respuesta que no se pudo leer.")
            }
        }
    }

    fun browseLocal() {
        request?.cancel()
        mutableState.value = ShelfState.Results(localClassics, local = true)
    }
}
