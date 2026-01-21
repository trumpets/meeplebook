package app.meeplebook.core.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@DslMarker
annotation class ScaffoldEffectDsl

@ScaffoldEffectDsl
class ScaffoldEffectScope(
    private val controller: ScaffoldController
) {
    fun setFab(state: FabState) {
        controller.setFab(state)
    }

    fun clearFab() {
        controller.clearFab()
    }

    internal var onDispose: (() -> Unit)? = null

    fun onDispose(block: () -> Unit) {
        onDispose = block
    }
}

@Composable
fun ScaffoldControllerEffect(
    key: Any? = Unit,
    block: ScaffoldEffectScope.() -> Unit
) {
    val controller = LocalScaffoldController.current

    // Create the scope once per key
    val scope = remember(key) {
        ScaffoldEffectScope(controller)
    }

    LaunchedEffect(key) {
        scope.block()
    }

    DisposableEffect(key) {
        onDispose {
            scope.onDispose?.invoke()
        }
    }
}

@Composable
fun FabEffect(
    key: Any? = Unit,
    state: FabState?
) {
    ScaffoldControllerEffect(key) {
        if (state != null) {
            setFab(state)
        } else {
            clearFab() // ← CRITICAL FIX
        }

//        onDispose {
//            clearFab()
//        }
    }
}