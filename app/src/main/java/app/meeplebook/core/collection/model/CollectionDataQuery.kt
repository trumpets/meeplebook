package app.meeplebook.core.collection.model

data class CollectionDataQuery(
    val searchQuery: String,
    val quickFilter: QuickFilter,
    val sort: CollectionSort
)