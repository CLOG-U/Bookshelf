package ec.edu.uleam.bookshelf.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ec.edu.uleam.bookshelf.data.Book

private val paper = Color(0xFFF8F5ED)
private val ink = Color(0xFF183C36)
private val muted = Color(0xFF53655D)
private val accent = Color(0xFF8A3829)

@Composable
fun BookshelfScreen(
    state: ShelfState, query: String, onSearch: (String) -> Unit, onLocal: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(query) }
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onSearch(input) }
    MaterialTheme(colorScheme = lightColorScheme(primary = ink, surface = paper, background = paper)) {
        Column(Modifier.fillMaxSize().background(paper).safeDrawingPadding().imePadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("BIBLIOTECA DIGITAL", color = accent, letterSpacing = 2.sp,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Bookshelf", color = ink, fontFamily = FontFamily.Serif,
                fontSize = 44.sp, lineHeight = 52.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = input, onValueChange = { input = it },
                    label = { Text("Título, autor o tema") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }))
                Button(onClick = submit, enabled = input.isNotBlank()) { Text("Buscar") }
            }
            Spacer(Modifier.height(16.dp))
            when (state) {
                ShelfState.Loading -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text("Consultando Google Books…", color = muted)
                }
                is ShelfState.Error -> {
                    HorizontalDivider()
                    Spacer(Modifier.height(22.dp))
                    Text("Consulta interrumpida", style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif, color = ink)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, color = muted)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSearch(query) }) { Text("Reintentar") }
                    TextButton(onClick = onLocal) { Text("Ver clásicos sin conexión") }
                }
                is ShelfState.Results -> {
                    val heading = if (state.local) "Clásicos sin conexión" else "Resultados de Google Books"
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(heading, Modifier.weight(1f), color = ink, fontFamily = FontFamily.Serif,
                            style = MaterialTheme.typography.titleLarge)
                        Text("${state.books.size} libros", color = muted, fontSize = 12.sp)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    if (state.books.isEmpty()) {
                        Text("No encontramos libros para «$query». Prueba con otro título o autor.", color = muted)
                    } else {
                        key(query, state.local) {
                            LazyVerticalGrid(columns = GridCells.Adaptive(140.dp),
                                contentPadding = PaddingValues(bottom = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                items(state.books, key = { it.id }) { book -> BookCard(book) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: Book) {
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(0.81f).clip(RoundedCornerShape(5.dp))) {
            var imageLoaded by remember(book.cover) { mutableStateOf(false) }
            if (!imageLoaded) DesignedCover(book)
            if (book.cover != null) {
                AsyncImage(model = book.cover, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                    onSuccess = { imageLoaded = true }, onError = { imageLoaded = false })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, color = ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
        if (book.author.isNotBlank()) Text(book.author, color = muted, fontSize = 12.sp,
            lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DesignedCover(book: Book) {
    val index = when (book.id) {
        "quijote" -> 0
        "frankenstein" -> 1
        "vuelta" -> 2
        "orgullo" -> 3
        else -> Math.floorMod(book.id.hashCode(), 4)
    }
    val colors = listOf(Color(0xFF10685B), Color(0xFF282E4B), Color(0xFF95462C), Color(0xFF68424B))
    val gold = Color(0xFFF4D190)
    Box(Modifier.fillMaxSize().background(colors[index])) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * .57f
            val cy = h * .55f
            drawLine(gold.copy(alpha = .55f), Offset(w * .075f, 0f), Offset(w * .075f, h), 1.5f)
            when (index) {
                0 -> {
                    drawCircle(gold, w * .095f, Offset(cx, cy))
                    repeat(4) { i ->
                        val a = i * Math.PI / 2 + .3
                        fun point(r: Float, angle: Double) = Offset(cx + r * kotlin.math.cos(angle).toFloat(),
                            cy + r * kotlin.math.sin(angle).toFloat())
                        val p = point(w * .38f, a)
                        val q = point(w * .31f, a + .44)
                        drawPath(Path().apply { moveTo(cx, cy); lineTo(p.x, p.y); lineTo(q.x, q.y); close() }, gold)
                    }
                }
                1 -> {
                    repeat(5) { i ->
                        val x = w * (.16f + i * .14f)
                        drawLine(gold.copy(alpha = .3f), Offset(x, h * .25f), Offset(x, h * .8f), 1.5f)
                    }
                    drawPath(Path().apply {
                        moveTo(w * .74f, h * .22f); lineTo(w * .34f, h * .55f)
                        lineTo(w * .52f, h * .55f); lineTo(w * .33f, h * .83f)
                        lineTo(w * .79f, h * .46f); lineTo(w * .60f, h * .47f); close()
                    }, gold)
                }
                2 -> {
                    val r = w * .32f
                    drawCircle(gold, r, Offset(cx, cy), style = Stroke(3f))
                    drawOval(gold, Offset(cx - r * .48f, cy - r), Size(r * .96f, r * 2), style = Stroke(2f))
                    drawLine(gold, Offset(cx - r, cy), Offset(cx + r, cy), 2f)
                    drawLine(gold, Offset(cx, cy - r), Offset(cx, cy + r), 2f)
                }
                3 -> {
                    repeat(6) { i ->
                        val a = i * Math.PI / 3
                        drawCircle(gold, w * .14f, Offset(cx + w * .17f * kotlin.math.cos(a).toFloat(),
                            cy + w * .17f * kotlin.math.sin(a).toFloat()), style = Stroke(2.5f))
                    }
                    drawCircle(gold, w * .075f, Offset(cx, cy))
                }
            }
        }
        Text(book.title, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 12.dp, end = 8.dp),
            fontFamily = FontFamily.Serif, color = Color.White, fontSize = 14.sp,
            lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
