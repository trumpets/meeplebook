package app.meeplebook.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

@Suppress("unused") // Lint loads this via reflection
class UiTextIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(UiTextInStringResourceDetector.ISSUE, UiTextInTextComposableDetector.ISSUE)

    override val api = CURRENT_API

    override val minApi = CURRENT_API

    override val vendor: Vendor
        get() = Vendor(
            vendorName = "MeepleBook",
            identifier = "app.meeplebook.lint",
            feedbackUrl = "https://github.com/trumpets/meeplebook"
        )
}