package app.meeplebook.feature.overview.effect

import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.feature.overview.OverviewBaseState
import app.meeplebook.feature.overview.OverviewEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewEffectProducerTest {

    private val producer = OverviewEffectProducer()

    @Test
    fun `refresh event produces refresh domain effect`() {
        val effects = producer.produce(OverviewBaseState(), OverviewEvent.ActionEvent.Refresh)

        assertEquals(listOf(OverviewEffect.Refresh), effects.effects)
        assertEquals(emptyList<OverviewUiEffect>(), effects.uiEffects)
    }

    @Test
    fun `log play event produces add play ui effect`() {
        val effects = producer.produce(OverviewBaseState(), OverviewEvent.ActionEvent.LogPlayClicked)

        assertEquals(listOf(OverviewUiEffect.OpenAddPlay), effects.uiEffects)
    }

    @Test
    fun `recent play event produces navigate to play ui effect`() {
        val effects = producer.produce(
            OverviewBaseState(),
            OverviewEvent.ActionEvent.RecentPlayClicked(PlayId.Local(9L))
        )

        assertEquals(listOf(OverviewUiEffect.NavigateToPlay(PlayId.Local(9L))), effects.uiEffects)
    }
}
