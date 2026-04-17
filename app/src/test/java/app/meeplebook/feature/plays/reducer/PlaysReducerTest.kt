package app.meeplebook.feature.plays.reducer

import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaysReducerTest {

    private val reducer = PlaysReducer()

    @Test
    fun `SearchChanged updates query in base state`() {
        val state = PlaysBaseState()

        val result = reducer.reduce(state, PlaysEvent.SearchChanged("azul"))

        assertEquals("azul", result.searchQuery)
    }

    @Test
    fun `SearchChanged preserves refreshing flag`() {
        val state = PlaysBaseState(isRefreshing = true)

        val result = reducer.reduce(state, PlaysEvent.SearchChanged("catan"))

        assertEquals("catan", result.searchQuery)
        assertEquals(true, result.isRefreshing)
    }

    @Test
    fun `ActionEvent leaves state unchanged`() {
        val state = PlaysBaseState(searchQuery = "root", isRefreshing = true)

        val result = reducer.reduce(state, PlaysEvent.ActionEvent.Refresh)

        assertEquals(state, result)
    }
}
