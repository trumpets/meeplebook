package app.meeplebook.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import kotlin.text.trimIndent

class UiTextInTextComposableDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UiTextInTextComposableDetector()

    override fun getIssues(): List<Issue> = listOf(UiTextInTextComposableDetector.ISSUE)

    fun testUiTextPassedAsNamedTextArgumentIsReported() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                composeTextStub(),
                kotlin(
                        """
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import app.meeplebook.core.ui.UiText
        
                    @Composable
                    fun Test(dateUiText: UiText) {
                        Text(text = dateUiText)
                    }
                    """
                )
            )
            .run()
            .expect(
                """
                src/test.kt:8: Error: UiText should not be passed directly to Text composable [UiTextInTextComposable]
                                        Text(text = dateUiText)
                                                    ~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    fun testUiTextPassedAsPositionalArgumentIsReported() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                composeTextStub(),
                kotlin(
                    """
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import app.meeplebook.core.ui.UiText
        
                    @Composable
                    fun Test(dateUiText: UiText) {
                        Text(dateUiText)
                    }
                    """
                )
            )
            .run()
            .expectErrorCount(1)
    }

    fun testUiTextAsStringIsAllowed() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                composeTextStub(),
                kotlin(
                    """
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import app.meeplebook.core.ui.UiText
        
                    @Composable
                    fun Test(dateUiText: UiText) {
                        Text(text = dateUiText.asString())
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    fun testAutofixReplacesTextWithUiTextText() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                composeTextStub(),
                kotlin(
                    """
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import app.meeplebook.core.ui.UiText
        
                    @Composable
                    fun Test(dateUiText: UiText) {
                        Text(text = dateUiText)
                    }
                    """
                )
            )
            .run()
            .expectFixDiffs(
                """
                Fix for src/test.kt line 8: Use UiTextText:
                @@ -8 +8 @@
                -                        Text(text = dateUiText)
                +                        UiTextText(text = dateUiText)
                """.trimIndent()
            )
    }

    fun testUiTextTextIsIgnored() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                composeTextStub(),
                uiTextTextStub(),
                kotlin(
                    """
                    import androidx.compose.runtime.Composable
                    import app.meeplebook.core.ui.UiText
                    import app.meeplebook.ui.components.UiTextText
        
                    @Composable
                    fun Test(dateUiText: UiText) {
                        UiTextText(dateUiText)
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }
}