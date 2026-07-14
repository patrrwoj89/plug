package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tvhub.skeleton.model.MediaItem
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.FocusableSurface
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Radius
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsStateWithLifecycle()

    if (item == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Item not found", style = AppTypography.body)
        }
        return
    }

    DetailContent(
        item = item!!,
        onPlay = { onNavigate(Screen.Player(item!!.id)) },
        modifier = modifier
    )
}

@Composable
private fun DetailContent(
    item: MediaItem,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            AsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.6f to AppColor.Black.copy(alpha = 0.6f),
                            1.0f to AppColor.Black
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(item.title, style = AppTypography.hero, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, style = AppTypography.body)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    FocusableSurface(
                        onClick = onPlay,
                        modifier = Modifier.height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
                        backgroundColor = AppColor.Accent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = AppColor.Black
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Play", style = AppTypography.button, color = AppColor.Black)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Overview", style = AppTypography.titleLarge)
            Text(item.description, style = AppTypography.body)
            Text("Genres: ${item.genres.joinToString(", ")}", style = AppTypography.caption)
            Text("Year: ${item.year}  •  Duration: ${item.duration}  •  Rating: ${item.rating}", style = AppTypography.caption)
        }
    }
}
