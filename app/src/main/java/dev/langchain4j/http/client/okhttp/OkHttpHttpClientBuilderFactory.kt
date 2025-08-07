package dev.langchain4j.http.client.okhttp

import dev.langchain4j.http.client.HttpClientBuilderFactory

/**
 * A factory for creating OkHttp-based HttpClient builders.
 * Implements the Langchain4j HttpClientBuilderFactory interface.
 */
class OkHttpHttpClientBuilderFactory : HttpClientBuilderFactory {

    /**
     * Creates a new instance of OkHttpClientBuilder.
     *
     * @return A new OkHttpClientBuilder.
     */
    override fun create(): OkHttpHttpClientBuilder {
        // Returns our custom OkHttp builder instance
        return OkHttpHttpClient.builder()
    }
}