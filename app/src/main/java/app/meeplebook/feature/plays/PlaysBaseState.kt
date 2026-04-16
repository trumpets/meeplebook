package app.meeplebook.feature.plays

data class PlaysBaseState(
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
)
