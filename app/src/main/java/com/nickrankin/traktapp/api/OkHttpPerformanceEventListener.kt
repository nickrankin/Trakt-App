/***
 * https://stackoverflow.com/questions/68475117/is-okhttp3-eventlistener-reliable-regarding-time-measurements-how-do-you-measu
 *
 *
 * **/

package com.nickrankin.traktapp.api

import android.util.Log
import okhttp3.*
import java.io.IOException

private const val TAG = "OkHttpPerformanceEventL"
class OkHttpPerformanceEventListener: EventListener() {
    @Volatile var sslHandshakeStartTimestamp: Long? = null
    @Volatile var requestBodyStartingTime: Long? = null
    @Volatile var responseBodyStartingTime: Long? = null

    var sslHandshakeTimeMS = 0L // resetting last ssl timestamp
    var requestBodyTimeMS = 0L
    var requestBodyPayload = 0L
    var responseBodyTimeMS = 0L
    var responseBodyPayload = 0L

    fun resetMetricData() {
        Log.d(TAG, "[RoundTrip] *** resetMetricData")
        sslHandshakeStartTimestamp = null
        requestBodyStartingTime = null
        responseBodyStartingTime = null
        sslHandshakeTimeMS = 0 // resetting last ssl timestamp
        requestBodyTimeMS = 0
        requestBodyPayload = 0
        responseBodyTimeMS = 0
        responseBodyPayload = 0
    }

    override fun callStart(call: Call) {
        Log.d(TAG, "[RoundTrip] *** callStart ${call.request().url}")
        resetMetricData()
    }

    override fun callEnd(call: Call) {
        Log.d(TAG, "[RoundTrip] *** callEnd ${call.request().url}")

    }

//    override fun secureConnectStart(call: Call) {
//        Log.d(TAG, "[RoundTrip] *** secureConnectStart. URL: ${call.request().url}")
//        sslHandshakeStartTimestamp = System.currentTimeMillis()
//    }
//
//    /**
//     * this method will be invoked after a ssl-handshake has been finished
//     */
//    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
//        Log.d(TAG, "[RoundTrip] *** secureConnectEnd. URL: ${call.request().url} ")
//        sslHandshakeStartTimestamp?.let {
//            sslHandshakeTimeMS = System.currentTimeMillis() - it
//        }
//    }
//
//
//    override fun connectionAcquired(call: Call, connection: Connection) {
//        Log.d(TAG, "[RoundTrip] *** connectionAcquired. URL: ${call.request().url} ")
//    }
//
//    override fun requestHeadersStart(call: Call) {
//        Log.d(TAG, "[RoundTrip] *** . URL: ${call.request().url} ")
//    }
//
//    override fun requestHeadersEnd(call: Call, request: Request) {
//        Log.d(TAG, "[RoundTrip] *** requestHeadersEnd. URL: ${call.request().url} ")
//    }

    override fun requestBodyStart(call: Call) {
        Log.d(TAG, "[RoundTrip] *** . URL: ${call.request().url} ")
        // we need to take this time after the ssl handshake has finished, due this callback is invoked to late and then only 3 ms
        // are recorded for the request time ,which is not correct
        requestBodyStartingTime = System.currentTimeMillis()

    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        requestBodyStartingTime?.let {
            requestBodyTimeMS = System.currentTimeMillis() - it
        }
        requestBodyPayload = byteCount
        Log.d(TAG, "[RoundTrip] *** requestBodyEnd, requestBodyStartingTime: $requestBodyStartingTime , requestBodyTimeMS: $requestBodyTimeMS , requestBodyPayload: $requestBodyPayload. URL: ${call.request().url} ")
    }

//    override fun responseHeadersStart(call: Call) {
//        Log.d(TAG, "[RoundTrip] *** responseHeadersStart ${call.request().url}. URL: ")
//    }

    override fun responseBodyStart(call: Call) {
        Log.d(TAG, "[RoundTrip] *** responseBodyStart ${call.request().url}. URL: ")
        responseBodyStartingTime = System.currentTimeMillis()
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        responseBodyStartingTime?.let {
            responseBodyTimeMS = System.currentTimeMillis() - it
        }
        responseBodyPayload = byteCount
        Log.d(TAG, "[RoundTrip] *** responseBodyEnd responseBodyStartingTime: $responseBodyStartingTime , responseBodyTimeMS: $responseBodyTimeMS , responseBodyPayload: $responseBodyPayload. URL: ${call.request().url} ")
    }

    override fun cacheHit(call: Call, response: Response) {
        Log.d(TAG, "[RoundTrip] *** cacheHit ${call.request().url}. URL: ")

    }

    override fun canceled(call: Call) {
        Log.e(TAG, "[RoundTrip] *** canceled ${call.request().url}. URL: ")

    }

}