package app.meeplebook.feature.plays.effect

sealed interface PlaysEffect {
    data object Refresh : PlaysEffect
}
