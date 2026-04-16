package app.meeplebook.feature.plays.effect

data class PlaysEffects(
    val effects: List<PlaysEffect>,
    val uiEffects: List<PlaysUiEffect>
) {
    companion object {
        /** Convenience value for transitions that produce no side effects. */
        val None = PlaysEffects(
            effects = emptyList(),
            uiEffects = emptyList()
        )
    }
}
