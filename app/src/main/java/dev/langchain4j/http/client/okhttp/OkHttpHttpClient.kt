package dev.langchain4j.http.client.okhttp

import dev.langchain4j.exception.HttpException
import dev.langchain4j.exception.TimeoutException
import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventParser
import dev.langchain4j.http.client.sse.ServerSentEventListener
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod // To check if body is allowed
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import okhttp3.OkHttpClient as OkHttp3Client // Alias to avoid name clash with our class

/**
 * An implementation of the Langchain4j HttpClient interface using OkHttp.
 * Supports standard synchronous requests and asynchronous Server-Sent Events (SSE).
 */
class OkHttpHttpClient(builder: OkHttpHttpClientBuilder) : HttpClient {

    private val okHttpClient: OkHttp3Client

    init {
        // Use provided builder or create a new one
        val okHttpBuilder = builder.okHttpClientBuilder() ?: OkHttp3Client.Builder()

        // Apply timeouts from our builder, potentially overriding base builder settings
        builder.applyTimeout(builder.connectTimeout()) { duration, unit ->
            okHttpBuilder.connectTimeout(duration, unit)
        }
        builder.applyTimeout(builder.readTimeout()) { duration, unit ->
            okHttpBuilder.readTimeout(duration, unit)
        }
        // OkHttp also has writeTimeout, often set same as readTimeout for simplicity,
        // but we stick to the original spec which only had read/connect.
        // builder.applyTimeout(builder.readTimeout()) { duration, unit ->
        //     okHttpBuilder.writeTimeout(duration, unit)
        // }

        this.okHttpClient = okHttpBuilder.build()
    }

    companion object {
        /**
         * Provides a static method to get a new builder instance.
         * Useful for Java interoperability and standard builder pattern.
         * @return A new OkHttpHttpClientBuilder.
         */
        @JvmStatic
        fun builder(): OkHttpHttpClientBuilder {
            return OkHttpHttpClientBuilder()
        }

        // Default content type if not specified in headers and body exists
        private val DEFAULT_MEDIA_TYPE = "text/plain; charset=utf-8".toMediaTypeOrNull()
    }

    /**
     * Executes a synchronous HTTP request.
     *
     * @param request The HttpRequest to execute.
     * @return A SuccessfulHttpResponse if the request was successful (2xx status code).
     * @throws TimeoutException if the request times out.
     * @throws HttpException if the server returns a non-2xx status code.
     * @throws IOException for other network or I/O errors.
     */
    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        val okHttpRequest = buildOkHttpRequest(request)
        try {
            okHttpClient.newCall(okHttpRequest).execute().use { response -> // 'use' ensures response is closed
                return handleSyncResponse(response)
            }
        } catch (e: SocketTimeoutException) {
            // Map OkHttp's specific timeout exception to Langchain4j's TimeoutException
            throw TimeoutException(e)
        } catch (e: IOException) {
            // Wrap other IOExceptions (could be network issues, etc.)
            // Consider if a more specific Langchain4j exception is appropriate,
            // but RuntimeException matches the original JdkHttpClient behavior for generic IO errors.
            throw RuntimeException("Error executing HTTP request: ${request.url()}", e)
        }
    }

    /**
     * Executes an asynchronous HTTP request, expecting Server-Sent Events (SSE).
     * The response stream is parsed using the provided parser, and events are
     * sent to the listener.
     *
     * @param request The HttpRequest to execute (should be configured for SSE).
     * @param parser The parser to process the SSE stream.
     * @param listener The listener to receive callbacks for events, open, close, and errors.
     */
    override fun execute(request: HttpRequest, parser: ServerSentEventParser, listener: ServerSentEventListener) {
        val okHttpRequest = buildOkHttpRequest(request)

        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val exception = if (e is SocketTimeoutException) {
                    TimeoutException(e)
                } else {
                    // Wrap generic IO error for clarity
                    RuntimeException("SSE request failed: ${request.url()}", e)
                }
                safeCallListener { listener.onError(exception) }
            }

            override fun onResponse(call: Call, response: Response) {
                // Use response ensures the body is closed even if exceptions occur
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        // Read body content for the HttpException
                        val errorBody = resp.body?.string() ?: "[No error body]"
                        val exception = HttpException(resp.code, errorBody)
                        safeCallListener { listener.onError(exception) }
                        return // Stop processing on error
                    }

                    // Notify listener that the connection is open
                    val successfulResponse = buildSuccessfulHttpResponse(resp, null) // Body not needed for onOpen
                    safeCallListener { listener.onOpen(successfulResponse) }

                    // Get the input stream for the parser
                    val inputStream: InputStream? = resp.body?.byteStream()

                    if (inputStream == null) {
                        val exception = RuntimeException("Successful SSE response but body is null: ${request.url()}")
                        safeCallListener { listener.onError(exception) }
                        safeCallListener { listener.onClose() } // Still close if stream is null? Yes, seems reasonable.
                        return
                    }

                    // Parse the stream
                    try {
                        // The parser is expected to handle the stream reading and event calling
                        parser.parse(inputStream, listener)
                    } catch (e: Exception) {
                        // Catch exceptions during parsing (could be IO or parsing logic error)
                        safeCallListener { listener.onError(e) }
                    } finally {
                        // Always notify listener that the stream/connection is closed after parsing attempt
                        safeCallListener { listener.onClose() }
                    }
                } // response.use closes the body/stream here automatically
            }
        })
    }

    /**
     * Builds an OkHttp Request object from a Langchain4j HttpRequest object.
     */
    private fun buildOkHttpRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url())

        // Add headers
        val headerBuilder = Headers.Builder()
        request.headers().forEach { (name, values) ->
            values?.forEach { value -> headerBuilder.add(name, value) }
        }
        builder.headers(headerBuilder.build())

        // Determine Content-Type from headers for the request body
        val contentTypeHeader = request.headers()["Content-Type"]?.firstOrNull()
        val mediaType = contentTypeHeader?.toMediaTypeOrNull()

        // Create request body if present
        val requestBody: RequestBody? = request.body()?.let { bodyContent ->
            // Use content type from header if available, otherwise default
            bodyContent.toRequestBody(mediaType ?: DEFAULT_MEDIA_TYPE)
        }

        // Set method and body (OkHttp requires non-null body for methods that expect one,
        // but RequestBody.create can handle empty strings if needed)
        val method = request.method().name
        if (requestBody == null && HttpMethod.requiresRequestBody(method)) {
            // Provide empty body for methods like POST/PUT if no body was specified
            builder.method(method, "".toRequestBody(mediaType))
        } else {
            builder.method(method, requestBody)
        }


        return builder.build()
    }

    /**
     * Processes a synchronous OkHttp Response.
     */
    private fun handleSyncResponse(response: Response): SuccessfulHttpResponse {
        val responseBodyString = response.body?.string() // Read body ONCE

        if (!response.isSuccessful) {
            throw HttpException(response.code, responseBodyString ?: "[No response body]")
        }

        return buildSuccessfulHttpResponse(response, responseBodyString)
    }

    /**
     * Builds a Langchain4j SuccessfulHttpResponse from an OkHttp Response.
     */
    private fun buildSuccessfulHttpResponse(response: Response, body: String?): SuccessfulHttpResponse {
        return SuccessfulHttpResponse.builder()
            .statusCode(response.code)
            .headers(response.headers.toMultimap()) // Convert OkHttp Headers to Map<String, List<String>>
            .body(body)
            .build()
    }

    /**
     * Safely executes a listener callback, catching and swallowing any exceptions.
     * This replaces the purpose of the old `ignoringExceptions` utility.
     * In a production scenario, you might want to log the caught exception here.
     */
    private inline fun safeCallListener(crossinline block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            // Log the exception from the listener callback if necessary
            // e.g., Log.e("OkHttpHttpClient", "Exception in listener callback", t)
            // As per original requirement, we don't propagate this exception.
        }
    }
}