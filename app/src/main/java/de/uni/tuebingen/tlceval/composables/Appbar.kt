package de.uni.tuebingen.tlceval.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

val DefaultToolbarHeight = 56.dp
val ToolbarBackButtonIconSize = 40.dp

@Composable
fun AppBar(
    title: String = "TLCyzer",
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable() RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        contentPadding = rememberInsetsPaddingValues(
            LocalWindowInsets.current.statusBars,
            applyBottom = false,
        ),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.background.copy(0.75f),
        contentColor = Color.Transparent,
        modifier = modifier.height(DefaultToolbarHeight)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth()
                .height(DefaultToolbarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (onNavigateBack != null)
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back Btn",
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                Text(
                    text = title,
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.overline,
                    fontSize = 26.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                content = actions,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}