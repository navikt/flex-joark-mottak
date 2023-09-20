package mock

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object SafMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return MockResponse().setResponseCode(400).setBody("Ingen saf")
    }
}
