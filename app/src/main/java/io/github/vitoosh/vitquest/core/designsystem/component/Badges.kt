package io.github.vitoosh.vitquest.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitoosh.vitquest.R

/** Where a displayed value comes from. Every value shown to the user must carry one. */
enum class DataSourceKind { MANUAL, ESTIMATE, SAMPLE, DEVICE }

@Composable
fun DataSourceTag(kind: DataSourceKind, modifier: Modifier = Modifier) {
    val label = when (kind) {
        DataSourceKind.MANUAL -> R.string.data_source_manual
        DataSourceKind.ESTIMATE -> R.string.data_source_estimate
        DataSourceKind.SAMPLE -> R.string.data_source_sample
        DataSourceKind.DEVICE -> R.string.data_source_device
    }
    VqBadge(
        text = stringResource(label),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}

@Composable
fun ComingSoonBadge(modifier: Modifier = Modifier) {
    VqBadge(
        text = stringResource(R.string.coming_soon),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    )
}

@Composable
fun VqBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
