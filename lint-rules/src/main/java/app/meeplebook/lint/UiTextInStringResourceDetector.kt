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
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression

class UiTextInStringResourceDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "UiTextInStringResource"
        private const val DESCRIPTION =
            "Passing a UiText object to stringResource or pluralStringResource is not allowed."
        private const val EXPLANATION =
            "Convert UiText to a String before passing it to stringResource or pluralStringResource, " +
                    "e.g., use UiText.asString(context)."
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

        // Fully-qualified UiText type
        private const val UITEXT_FQN = "app.meeplebook.core.ui.UiText"
    }

    override fun getApplicableMethodNames(): List<String> =
        listOf("stringResource", "pluralStringResource")

    // TODO detect Text(text = UiText) as well
    // TODO update lint tests with those cases
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
                    "UiText should not be passed directly to $methodName; convert it to a String first",
                    createFix(methodName, node, arg, context)
                )
            }
        }
    }

    private fun isSafeUiTextConversion(
        context: JavaContext,
        expression: UExpression
    ): Boolean {
        if (expression !is UCallExpression) return false
        if (expression.methodName != "asString") return false

        val receiver = expression.receiver ?: return false
        val type = receiver.getExpressionType()
        val cls = type?.let { context.evaluator.getTypeClass(it) }

        return cls != null &&
                context.evaluator.extendsClass(cls, UITEXT_FQN, false)
    }

    private fun isUiTextExpression(
        context: JavaContext,
        expression: UExpression
    ): Boolean {

        // Case 1: direct constructor call
        if (expression is UCallExpression) {
            val type = expression.getExpressionType()
            val cls = type?.let { context.evaluator.getTypeClass(it) }
            if (cls != null &&
                context.evaluator.extendsClass(cls, UITEXT_FQN, false)
            ) {
                return true
            }
        }

        // Case 2: variable reference
        if (expression is UReferenceExpression) {
            val resolved = expression.resolve()

            // val dateUiText: UiText
            if (resolved is PsiVariable) {
                val type = resolved.type
                val cls = context.evaluator.getTypeClass(type)
                if (cls != null &&
                    context.evaluator.extendsClass(cls, UITEXT_FQN, false)
                ) {
                    return true
                }
            }
        }

        return false
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