package ec.edu.uleam.bookshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.edu.uleam.bookshelf.data.BooksApi
import ec.edu.uleam.bookshelf.data.GoogleBooksRepository
import ec.edu.uleam.bookshelf.ui.BookshelfScreen
import ec.edu.uleam.bookshelf.ui.ShelfViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: ShelfViewModel = viewModel {
                val api = Retrofit.Builder()
                    .baseUrl("https://www.googleapis.com/books/v1/")
                    .addConverterFactory(GsonConverterFactory.create()).build()
                    .create(BooksApi::class.java)
                ShelfViewModel(GoogleBooksRepository(api, BuildConfig.BOOKS_API_KEY.ifBlank { null }))
            }
            val state by model.state.collectAsStateWithLifecycle()
            BookshelfScreen(state, model.lastQuery, model::search, model::browseLocal)
        }
    }
}
