package app.meeplebook.core.ui.scaffold

import androidx.compose.runtime.staticCompositionLocalOf

val LocalScaffoldController =
    staticCompositionLocalOf<ScaffoldController> {
        error("ScaffoldController not provided")
    }