package com.example.eventnote.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.rememberHiltViewModel

private val appBarTitlesForNavRail = mapOf(1 to "首页", 2 to "日历")

@Composable fun NavigationGraph(viewModel: EventNoteViewModel = rememberHiltViewModel()) {}
