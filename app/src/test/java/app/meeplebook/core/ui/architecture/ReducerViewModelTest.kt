package app.meeplebook.core.ui.architecture

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReducerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dispatchEvent reduces state before producing effects`() = runTest {
        val viewModel = TestReducerViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(TestEvent.Increment)
            advanceUntilIdle()

            assertEquals(CounterState(count = 1, label = "count=1"), viewModel.reducerState.value)
            assertEquals(TestUiEffect.ShowMessage("count=1"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatchEvent routes domain effects to handler`() = runTest {
        val viewModel = TestReducerViewModel()

        viewModel.onEvent(TestEvent.RefreshClicked)
        advanceUntilIdle()

        assertEquals(listOf(TestDomainEffect.Refresh), viewModel.handledDomainEffects)
    }

    @Test
    fun `late collectors do not receive replayed ui effects`() = runTest {
        val viewModel = TestReducerViewModel()

        viewModel.onEvent(TestEvent.Increment)
        advanceUntilIdle()

        viewModel.uiEffect.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateBaseState supports async effect results`() = runTest {
        val viewModel = TestReducerViewModel()

        viewModel.markLoading()

        assertTrue(viewModel.reducerState.value.isLoading)
    }

    private class TestReducerViewModel : ReducerViewModel<
        CounterState,
        TestEvent,
        TestDomainEffect,
        TestUiEffect
    >(
        initialState = CounterState(),
        reducer = Reducer { state, event ->
            when (event) {
                TestEvent.Increment -> state.copy(count = state.count + 1, label = "count=${state.count + 1}")
                TestEvent.RefreshClicked -> state
            }
        },
        effectProducer = TestEffectProducer()
    ) {

        val handledDomainEffects = mutableListOf<TestDomainEffect>()
        val reducerState: StateFlow<CounterState> = baseState

        fun onEvent(event: TestEvent) {
            dispatchEvent(event)
        }

        fun markLoading() {
            updateBaseState { it.copy(isLoading = true) }
        }

        override fun handleDomainEffect(effect: TestDomainEffect) {
            handledDomainEffects += effect
        }
    }

    private class TestEffectProducer :
        EffectProducer<CounterState, TestEvent, TestDomainEffect, TestUiEffect>() {

        override fun produceEffects(
            newState: CounterState,
            event: TestEvent
        ): ProducedEffects<TestDomainEffect, TestUiEffect> {
            return when (event) {
                TestEvent.Increment ->
                    ProducedEffects(
                        uiEffects = listOf(TestUiEffect.ShowMessage(newState.label))
                    )

                TestEvent.RefreshClicked ->
                    ProducedEffects(
                        effects = listOf(TestDomainEffect.Refresh)
                    )
            }
        }
    }

    private data class CounterState(
        val count: Int = 0,
        val label: String = "",
        val isLoading: Boolean = false
    )

    private sealed interface TestEvent {
        data object Increment : TestEvent
        data object RefreshClicked : TestEvent
    }

    private sealed interface TestDomainEffect {
        data object Refresh : TestDomainEffect
    }

    private sealed interface TestUiEffect {
        data class ShowMessage(val message: String) : TestUiEffect
    }
}
