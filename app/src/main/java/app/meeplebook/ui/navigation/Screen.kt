package app.meeplebook.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
data class Home(val refreshOnLogin: Boolean = false)
