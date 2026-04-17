package app.meeplebook.core.util

import java.text.DecimalFormat

object ScoreFormatter {
    private val formatter = DecimalFormat("#.##")

    fun format(score: Double): String = formatter.format(score)
}