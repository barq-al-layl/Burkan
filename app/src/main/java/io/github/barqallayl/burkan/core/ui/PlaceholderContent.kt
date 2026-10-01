package io.github.barqallayl.burkan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.barqallayl.burkan.R

/** A destination whose screen is not built yet: its title, and a line saying so. */
@Composable
fun PlaceholderContent(title: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.placeholder_not_built))
        }
    }
}
