package app.meeplebook.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class UiTextInTextComposableDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "UiTextInTextComposable"

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = "UiText passed directly to Text composable",
            explanation =
                "Text composable expects a String. " +
                        "Pass a String [UiText.asString()] or use UiTextText instead.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                UiTextInTextComposableDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        // Defensive: make sure this is *compose* Text
        val containingClass = method.containingClass?.qualifiedName ?: return
        if (!containingClass.startsWith("androidx.compose.material") &&
            !containingClass.startsWith("androidx.compose.material3")
        ) return

        val textArg = findTextArgument(node) ?: return

        if (isSafeUiTextConversion(context, textArg)) return
        if (!isUiTextExpression(context, textArg)) return

        context.report(
            ISSUE,
            node,
            context.getNameLocation(textArg),
            "UiText should not be passed directly to Text composable",
            createFix(context, node)
        )
    }

    private fun createFix(
        context: JavaContext,
        call: UCallExpression
    ): LintFix? {

        val callPsi = call.sourcePsi ?: return null
        val callText = callPsi.text

        return LintFix.create()
            .replace()
            .name("Use UiTextText")
            .range(context.getLocation(callPsi))
            .with(callText.replaceFirst("Text", "UiTextText"))
            .build()
    }
}