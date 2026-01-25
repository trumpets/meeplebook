package app.meeplebook.lint

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression

// Fully-qualified UiText type
private const val UITEXT_FQN = "app.meeplebook.core.ui.UiText"

fun isSafeUiTextConversion(
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

fun isUiTextExpression(
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

fun findTextArgument(node: UCallExpression): UExpression? {
    val psiCall = node.sourcePsi as? org.jetbrains.kotlin.psi.KtCallExpression
        ?: return null

    val valueArgs = psiCall.valueArguments

    // Named argument: Text(text = ...)
    valueArgs.firstOrNull { arg ->
        arg.getArgumentName()?.asName?.identifier == "text"
    }?.let { namedArg ->
        return node.valueArguments[valueArgs.indexOf(namedArg)]
    }

    // Positional: Text("hello")
    return node.valueArguments.firstOrNull()
}