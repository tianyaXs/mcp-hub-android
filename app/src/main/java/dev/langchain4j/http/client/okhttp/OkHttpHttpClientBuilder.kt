package dev.langchain4j.http.client.okhttp

import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpClientBuilder
import okhttp3.OkHttpClient
import java.time.Duration
import kotlin.math.min

// Define a reasonable maximum timeout in milliseconds for OkHttp (avoids overflow issues)
private const val MAX_TIMEOUT_MILLIS = Int.MAX_VALUE.toLong()

/**
 * Builder for creating instances of OkHttpHttpClient.
 * Allows configuration of connect and read timeouts, and optionally accepts
 * a pre-configured OkHttpClient.Builder.
 */
class OkHttpHttpClientBuilder : HttpClientBuilder {

    private var okHttpClientBuilder: OkHttpClient.Builder? = null
    private var connectTimeout: Duration? = null
    private var readTimeout: Duration? = null

    /**
     * Returns the currently configured OkHttpClient.Builder, if one was provided.
     *
     * @return The OkHttpClient.Builder or null.
     */
    fun okHttpClientBuilder(): OkHttpClient.Builder? {
        return okHttpClientBuilder
    }

    /**
     * Sets a custom OkHttpClient.Builder to use as a base.
     * Timeouts configured via this builder's methods will override those
     * set on the provided OkHttpClient.Builder instance during the build process.
     *
     * @param okHttpClientBuilder The OkHttpClient.Builder instance.
     * @return This builder instance for chaining.
     */
    fun okHttpClientBuilder(okHttpClientBuilder: OkHttpClient.Builder): OkHttpHttpClientBuilder {
        this.okHttpClientBuilder = okHttpClientBuilder
        return this
    }

    /**
     * Returns the configured connection timeout.
     *
     * @return The connection timeout Duration or null.
     */
    override fun connectTimeout(): Duration? {
        return connectTimeout
    }

    /**
     * Sets the connection timeout for the HttpClient.
     *
     * @param connectTimeout The connection timeout Duration.
     * @return This builder instance for chaining.
     */
    override fun connectTimeout(connectTimeout: Duration): OkHttpHttpClientBuilder {
        this.connectTimeout = connectTimeout
        return this
    }

    /**
     * Returns the configured read timeout.
     *
     * @return The read timeout Duration or null.
     */
    override fun readTimeout(): Duration? {
        return readTimeout
    }

    /**
     * Sets the read timeout for the HttpClient.
     *
     * @param readTimeout The read timeout Duration.
     * @return This builder instance for chaining.
     */
    override fun readTimeout(readTimeout: Duration): OkHttpHttpClientBuilder {
        this.readTimeout = readTimeout
        return this
    }

    /**
     * Builds the OkHttpHttpClient instance based on the configured settings.
     * It will use the provided OkHttpClient.Builder if available, or create a new one.
     * Configured connect and read timeouts will be applied to the builder before creating
     * the final OkHttpClient instance.
     *
     * @return A configured OkHttpHttpClient instance.
     */
    override fun build(): HttpClient { // Return the interface type
        return OkHttpHttpClient(this)
    }

    // Internal helper to apply duration safely to OkHttp builder
    internal fun applyTimeout(duration: Duration?, applier: (Long, java.util.concurrent.TimeUnit) -> Unit) {
        duration?.let {
            val millis = min(it.toMillis(), MAX_TIMEOUT_MILLIS) // Prevent overflow
            if (millis > 0) { // OkHttp requires positive timeout values
                applier(millis, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }
}