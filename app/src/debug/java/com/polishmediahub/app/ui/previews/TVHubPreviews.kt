package com.polishmediahub.app.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.theme.TVHubTheme

@Preview(device = "id:tv_1080p", name = "Focusable surface")
@Composable
fun FocusableSurfacePreview() {
    TVHubTheme {
        FocusableSurface(onClick = {}) {
            androidx.compose.material3.Text("Focus me")
        }
    }
}

@Preview(device = "id:tv_1080p", name = "Empty state")
@Composable
fun EmptyStatePreview() {
    TVHubTheme {
        com.polishmediahub.app.ui.components.EmptyState(message = "No content yet")
    }
}
