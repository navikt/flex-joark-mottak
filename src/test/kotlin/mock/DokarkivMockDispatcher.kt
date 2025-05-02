package mock

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest

object DokarkivMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (responseQueue.peek() != null) {
            return responseQueue.take()
        }

        return when {
            request.requestUrl!!.encodedPath.startsWith("/rest/journalpostapi/v1/journalpost/") ->
                MockResponse().setResponseCode(
                    200,
                )

            else ->
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Har ikke implemetert dokarkiv mock api for ${request.requestUrl}")
        }
    }
}
