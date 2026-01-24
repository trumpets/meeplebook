package app.meeplebook.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

@Suppress("unused") // Lint loads this via reflection
class UiTextIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(UiTextInStringResourceDetector.ISSUE)

    override val api = CURRENT_API

    override val minApi = CURRENT_API
}