package mock

import mockwebserver3.MockResponse
import mockwebserver3.QueueDispatcher
import mockwebserver3.RecordedRequest

object DokarkivMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (responseQueue.peek() != null) {
            return responseQueue.take()
        }

        return when {
            request.url.encodedPath.startsWith("/rest/journalpostapi/v1/journalpost/") ->
                MockResponse(code = 200)

            else ->
                MockResponse(
                    code = 404,
                    body = "Har ikke implemetert dokarkiv mock api for ${request.url}",
                )
        }
    }
}
