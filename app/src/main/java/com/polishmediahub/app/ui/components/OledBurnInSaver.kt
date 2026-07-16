package com.polishmediahub.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.polishmediahub.app.navigation.LocalPlayerViewModel
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.viewmodel.HomeViewModel
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlin.random.Random

private const val IDLE_THRESHOLD_MS = 300_000L
private const val CLOCK_UPDATE_MS = 1_000L
private const val POSTER_COUNT = 4

@Composable
fun OledBurnInSaver(
    lastInteractionMs: Long,
    onWake: () -> Unit,
    playerViewModel: PlayerViewModel = LocalPlayerViewModel.current,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val inAppPip by playerViewModel.videoPipManager.isInPipMode.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

    var tick by remember { mutableIntStateOf(0) }
    var clockTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(tick) {
        clockTime = System.currentTimeMillis()
        kotlinx.coroutines.delay(CLOCK_UPDATE_MS)
        tick++
    }

    val active = clockTime - lastInteractionMs >= IDLE_THRESHOLD_MS && !isPlaying && !inAppPip
    if (!active) return

    val posters = remember(homeState) {
        val urls = mutableListOf<String>()
        homeState.featured.firstOrNull()?.posterUrl?.let { urls.add(it) }
        homeState.featured.firstOrNull()?.backdropUrl?.let { urls.add(it) }
        homeState.categories.forEach { cat ->
            cat.items.forEach { item ->
                item.posterUrl?.let { urls.add(it) }
                if (urls.size >= POSTER_COUNT * 2) return@forEach
            }
        }
        urls.take(POSTER_COUNT * 2)
    }

    val posterStates = remember { mutableStateListOf<PosterState>() }
    LaunchedEffect(posters) {
        if (posterStates.isEmpty() && posters.isNotEmpty()) {
            repeat(POSTER_COUNT) { index ->
                val url = posters.getOrNull(index) ?: posters.firstOrNull() ?: ""
                posterStates.add(
                    PosterState(
                        url = url,
                        x = Random.nextFloat() * 800f,
                        y = Random.nextFloat() * 400f,
                        vx = (Random.nextFloat() - 0.5f) * 2f,
                        vy = (Random.nextFloat() - 0.5f) * 2f
                    )
                )
            }
        }
    }

    LaunchedEffect(tick) {
        val width = 1000f
        val height = 600f
        posterStates.forEach { state ->
            state.x = (state.x + state.vx).coerceIn(0f, width)
            state.y = (state.y + state.vy).coerceIn(0f, height)
            if (state.x <= 0f || state.x >= width) state.vx *= -1
            if (state.y <= 0f || state.y >= height) state.vy *= -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onWake() }
            .focusable(true)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onWake()
                    true
                } else {
                    false
                }
            }
    ) {
        posterStates.forEach { state ->
            val animatedX by animateFloatAsState(targetValue = state.x, label = "poster_x")
            val animatedY by animateFloatAsState(targetValue = state.y, label = "poster_y")
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .offset {
                        IntOffset(animatedX.roundToInt(), animatedY.roundToInt())
                    }
                    .clip(RoundedCornerShape(Radius.md))
                    .alpha(0.25f)
            )
        }

        Text(
            text = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).format(Date(clockTime)),
            style = AppTypography.hero,
            color = AppColor.OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private data class PosterState(
    val url: String,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float
)
