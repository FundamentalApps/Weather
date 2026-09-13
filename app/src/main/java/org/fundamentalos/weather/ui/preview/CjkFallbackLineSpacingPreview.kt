package org.fundamentalos.weather.ui.preview

import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Preview(showBackground = true)
@Composable
private fun CjkFallbackLineSpacingPreview() {
    val style = TextStyle(
        fontSize = 24.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Column(
        modifier = Modifier
            .padding(8.dp)
            .border(1.dp, Color.Gray)
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .drawBehind {
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Hello", "Hello 中文", "中文").forEach { text ->
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        modifier = Modifier.background(Color(0x33FBC02D)),
                        style = style,
                        maxLines = 1
                    )
                }
            }
            AndroidView(
                modifier = Modifier.background(Color(0x33FBC02D)),
                factory = { context ->
                    TextView(context).apply {
                        includeFontPadding = false
                        if (Build.VERSION.SDK_INT >= 28) {
                            isFallbackLineSpacing = false
                        }
                    }
                },
                update = { view ->
                    view.text = "中文"
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    view.includeFontPadding = false
                    if (Build.VERSION.SDK_INT >= 28) {
                        view.isFallbackLineSpacing = false
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Material3CjkContainersPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("中文") }
                AssistChip(onClick = {}, label = { Text("中文") })
                FloatingActionButton(onClick = {}) { Text("中文") }
                ExtendedFloatingActionButton(onClick = {}) { Text("中文") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) {
                    Icon(Icons.Default.Add, null)
                    Text("中文")
                }
                AssistChip(onClick = {}, label = {
                    Icon(Icons.Default.Add, null)
                    Text("中文")
                })
                ExtendedFloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, null)
                    Text("中文")
                }
            }
        }
    }
}
