package app.meeplebook.core.plays.remote

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.remote.dto.RemotePlayDto

/**
 * Fake implementation of [PlaysRemoteDataSource] for testing.
 */
class FakePlaysRemoteDataSource : PlaysRemoteDataSource {

    var playsToReturn: List<RemotePlayDto> = emptyList()
    var playsToReturnByPage: Map<Int, List<RemotePlayDto>> = emptyMap()
    var shouldThrowException: Exception? = null
    var uploadExceptionsByLocalId: Map<Long, Exception> = emptyMap()
    var uploadedPlays: List<Play> = emptyList()
        private set
    var uploadedLocalIds: List<Long> = emptyList()
        private set
    var uploadedRemoteIdsByLocalId: Map<Long, Long> = emptyMap()
    var fetchPlaysCalled = false
    var lastFetchUsername: String? = null
    var lastFetchPage: Int? = null

    override suspend fun fetchPlays(username: String, page: Int?): List<RemotePlayDto> {
        fetchPlaysCalled = true
        lastFetchUsername = username
        lastFetchPage = page

        shouldThrowException?.let { throw it }

        if (playsToReturnByPage.isNotEmpty() && page != null) {
            return playsToReturnByPage[page] ?: emptyList()
        }

        return playsToReturn
    }

    override suspend fun uploadPlay(play: Play): Long {
        uploadedPlays = uploadedPlays + play
        uploadedLocalIds = uploadedLocalIds + play.playId.localId

        uploadExceptionsByLocalId[play.playId.localId]?.let { throw it }

        return uploadedRemoteIdsByLocalId[play.playId.localId]
            ?: (play.playId as? PlayId.Remote)?.remoteId
            ?: (10_000L + play.playId.localId)
    }
}
