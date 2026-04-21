package com.example.eventnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventNoteTheme {
                // 启动首页列表页面
              }
            }
        }
    }

    @Composable
    private fun EventNoteTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = lightColorScheme(primary = MaterialTeal, secondary = MaterialPuce)
        )
    }
}
