@Composable
internal fun AlertAction(
    label: String,
    emphasised: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 16.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        fontWeight = if (emphasised) androidx.compose.ui.text.font.FontWeight.W600
                     else androidx.compose.ui.text.font.FontWeight.Normal,
    )
}
