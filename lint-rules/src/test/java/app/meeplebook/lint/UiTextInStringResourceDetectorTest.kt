package app.meeplebook.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class UiTextInStringResourceDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UiTextInStringResourceDetector()

    override fun getIssues(): List<Issue> = listOf(UiTextInStringResourceDetector.ISSUE)

    // ---------------------------------------------------------------------
    // stringResource
    // ---------------------------------------------------------------------

    fun testUiTextPassedToStringResourceIsReported() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.stringResource
                    import app.meeplebook.core.ui.UiText

                    @Composable
                    fun Test() {
                        val dateUiText: UiText = UiText.Plain("2026-01-24")
                        val text = stringResource(
                            R.string.sync_plays_failed_error,
                            dateUiText
                        )
                    }
                    """
                )
            )
            .run()
            .expect(
                """
                src/test/test.kt:13: Error: UiText should not be passed directly to stringResource; convert it to a String first [UiTextInStringResource]
                                            dateUiText
                                            ~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    // ---------------------------------------------------------------------
    // pluralStringResource
    // ---------------------------------------------------------------------

    fun testUiTextPassedToPluralStringResourceIsReported() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.pluralStringResource
                    import app.meeplebook.core.ui.UiText

                    @Composable
                    fun Test() {
                        val uiText: UiText = UiText.Plain("foo")
                        val text = pluralStringResource(
                            R.plurals.items_count,
                            2,
                            uiText
                        )
                    }
                    """
                )
            )
            .run()
            .expect(
                """
                src/test/test.kt:14: Error: UiText should not be passed directly to pluralStringResource; convert it to a String first [UiTextInStringResource]
                                            uiText
                                            ~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    // ---------------------------------------------------------------------
    // Safe cases
    // ---------------------------------------------------------------------

    fun testStringLiteralPassedToStringResourceIsAllowed() {
        lint()
            .files(
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.stringResource

                    @Composable
                    fun Test() {
                        val text = stringResource(
                            R.string.sync_plays_failed_error,
                            "2026-01-24"
                        )
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    fun testStringVariablePassedToStringResourceIsAllowed() {
        lint()
            .files(
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.stringResource

                    @Composable
                    fun Test() {
                        val date: String = "2026-01-24"
                        val text = stringResource(
                            R.string.sync_plays_failed_error,
                            date
                        )
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    fun testUiTextConvertedToStringBeforePassingToStringResourceIsAllowed() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                resourceStubs(),
                resourceIdsStub(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.stringResource
                    import app.meeplebook.core.ui.UiText

                    @Composable
                    fun Test() {
                        val dateUiText: UiText = UiText.Plain("2026-01-24")
                        val text = stringResource(
                            R.string.sync_plays_failed_error,
                            dateUiText.asString()
                        )
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    // ---------------------------------------------------------------------
    // Test both quick-fixes are offered for stringResource
    // ---------------------------------------------------------------------
    fun testUiTextQuickFixesForStringResource() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.stringResource
                    import app.meeplebook.core.ui.UiText

                    @Composable
                    fun Test() {
                        val dateUiText: UiText = UiText.Dynamic("2026-01-24")
                        val text = stringResource(
                            R.string.sync_plays_failed_error,
                            dateUiText
                        )
                    }
                    """
                )
            )
            .run()
            .expect(
                """
                src/test/test.kt:13: Error: UiText should not be passed directly to stringResource; convert it to a String first [UiTextInStringResource]
                                            dateUiText
                                            ~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/test/test.kt line 13: Use uiTextRes:
                @@ -11 +11 @@
                -                        val text = stringResource(
                +                        val text = uiTextRes(
                Fix for src/test/test.kt line 13: Convert UiText to String:
                @@ -13 +13 @@
                -                            dateUiText
                +                            dateUiText.asString()
                """.trimIndent()
            )
    }

    // ---------------------------------------------------------------------
    // Test both quick-fixes for pluralStringResource
    // ---------------------------------------------------------------------
    fun testUiTextQuickFixesForPluralStringResource() {
        lint()
            .files(
                uiTextStub(),
                composeStubs(),
                resourceStubs(),
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.res.pluralStringResource
                    import app.meeplebook.core.ui.UiText

                    @Composable
                    fun Test() {
                        val uiText: UiText = UiText.Dynamic("foo")
                        val text = pluralStringResource(
                            R.plurals.items_count,
                            2,
                            uiText
                        )
                    }
                    """
                )
            )
            .run()
            .expect(
                """
                src/test/test.kt:14: Error: UiText should not be passed directly to pluralStringResource; convert it to a String first [UiTextInStringResource]
                                            uiText
                                            ~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/test/test.kt line 14: Use uiTextPlural:
                @@ -11 +11 @@
                -                        val text = pluralStringResource(
                +                        val text = uiTextPlural(
                Fix for src/test/test.kt line 14: Convert UiText to String:
                @@ -14 +14 @@
                -                            uiText
                +                            uiText.asString()
                """.trimIndent()
            )
    }

    // ---------------------------------------------------------------------
    // Test stubs
    // ---------------------------------------------------------------------

    private fun uiTextStub() = kotlin(
        """
        package app.meeplebook.core.ui

        sealed class UiText {
            data class Plain(val value: String) : UiText()
            data class Res(val resId: Int) : UiText()
        }
        """
    )

    private fun composeStubs() = kotlin(
        """
        package androidx.compose.runtime

        annotation class Composable
        """
    )

    private fun resourceStubs() = kotlin(
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

    private fun resourceIdsStub() = kotlin(
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
}
