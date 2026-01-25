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
import org.jetbrains.uast.UExpression

class UiTextInStringResourceDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "UiTextInStringResource"
        private const val DESCRIPTION =
            "Passing a UiText object to stringResource or pluralStringResource is not allowed."
        private const val EXPLANATION =
            "Convert UiText to a String before passing it to stringResource or pluralStringResource, " +
                    "e.g., use UiText.asString(context). Or use uiTextRes / uiTextPlural instead of stringResource / pluralStringResource."
        private val IMPLEMENTATION = Implementation(
            UiTextInStringResourceDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = DESCRIPTION,
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }

    override fun getApplicableMethodNames(): List<String> =
        listOf("stringResource", "pluralStringResource")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = method.name
        val args = node.valueArguments

        // Determine which arguments to check
        val argsToCheck: List<UExpression> = when (methodName) {
            "stringResource" -> args.drop(1) // skip the first (resource ID)
            "pluralStringResource" -> if (args.size > 1) args.drop(2) else emptyList() // skip resource ID + count
            else -> emptyList()
        }

        for (arg in argsToCheck) {
            if (isSafeUiTextConversion(context, arg)) continue

            if (isUiTextExpression(context, arg)) {
                context.report(
                    ISSUE,
                    node,
                    context.getNameLocation(arg),
                    "UiText should not be passed directly to $methodName; convert it to a String first or use uiTextRes / uiTextPlural",
                    createFix(methodName, node, arg, context)
                )
            }
        }
    }

    private fun createFix(
        methodName: String,
        call: UCallExpression,
        arg: UExpression,
        context: JavaContext
    ): LintFix? {

        val originalTextForAsStringFix = arg.sourcePsi?.text ?: return null

        val asStringFix = LintFix.create()
            .replace()
            .name("Convert UiText to String")
            .text(originalTextForAsStringFix)
            .with("$originalTextForAsStringFix.asString()")
            .build()

        val uiTextWrapperFix = when (methodName) {
            "stringResource", "pluralStringResource" -> {
                val methodIdentifier = call.methodIdentifier ?: return null

                val replacementMethod = when (methodName) {
                    "stringResource" -> "uiTextRes"
                    "pluralStringResource" -> "uiTextPlural"
                    else -> methodName
                }

                // Keep all arguments, just change the method name
                LintFix.create()
                    .replace()
                    .name("Use $replacementMethod")
                    .range(context.getLocation(methodIdentifier))
                    .with(replacementMethod)
                    .build()
            }

            else -> null
        }

        return if (uiTextWrapperFix != null) {
            LintFix.create()
                .alternatives(
                    uiTextWrapperFix,
                    asStringFix
                )
        } else {
            asStringFix
        }
    }
}