package app.meeplebook.core.plays.remote

/**
 * Raised when BGG rejects an uploaded play or returns a response that cannot be treated as success.
 */
class PlayUploadException(
    message: String
) : Exception(message)
