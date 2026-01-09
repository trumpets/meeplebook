package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.DomainCollectionItem

data class DomainCollectionSection(
    val key: Char,
    val items: List<DomainCollectionItem>
)