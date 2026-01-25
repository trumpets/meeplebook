package app.meeplebook.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin

fun uiTextStub() = kotlin(
    """
        package app.meeplebook.core.ui

        sealed class UiText {
            data class Plain(val value: String) : UiText()
            data class Res(val resId: Int) : UiText()
        }
        """
)

fun composeStubs() = kotlin(
    """
        package androidx.compose.runtime

        annotation class Composable
        """
)

fun resourceStubs() = kotlin(
    """
        package androidx.compose.ui.res

        fun stringResource(id: Int, vararg formatArgs: Any): String {
            return ""
        }

        fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String {
            return ""
        }
        """
)

fun resourceIdsStub() = kotlin(
    """
        package test

        object R {
            object string {
                const val sync_plays_failed_error: Int = 1
            }
            object plurals {
                const val items_count: Int = 2
            }
        }
        """
)

fun composeTextStub() = kotlin(
    """
        package androidx.compose.material3
    
        import androidx.compose.runtime.Composable
    
        @Composable
        fun Text(text: String) {}
        """
)

fun uiTextTextStub() = kotlin(
    """
        package app.meeplebook.ui.components

        import androidx.compose.runtime.Composable
        import app.meeplebook.core.ui.UiText
        
        @Composable
        fun UiTextText(text: UiText) {}
        """
)