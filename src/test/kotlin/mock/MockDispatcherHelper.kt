package mock

import mockwebserver3.MockResponse
import org.springframework.http.MediaType

fun withContentTypeApplicationJson(createMockResponse: () -> MockResponse): MockResponse =
    createMockResponse()
        .newBuilder()
        .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()
