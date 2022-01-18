package com.nickrankin.traktapp.api.services.trakt

import com.uwetrottmann.trakt5.entities.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TmAuth {
    @POST("oauth/token")
    suspend fun exchangeCodeForAccessToken(
        @Body tokenRequest: AccessTokenRequest?
    ): AccessToken

    @POST("oauth/token")
    suspend fun refreshAccessToken(
        @Body refreshRequest: AccessTokenRefreshRequest?
    ): AccessToken

    /**
     * Generate new codes to start the device authentication process.
     * The `device_code` and `interval` will be used later to poll for the `access_token`.
     * The `user_code` and `verification_url` should be presented to the user.
     * @param clientId Application Client Id
     */
    @POST("oauth/device/code")
    suspend fun generateDeviceCode(
        @Body clientId: ClientId?
    ): DeviceCode

    /**
     * Use the `device_code` and poll at the `interval` (in seconds) to check if the user has
     * authorized you app. Use `expires_in` to stop polling after that many seconds, and gracefully
     * instruct the user to restart the process.
     * **It is important to poll at the correct interval and also stop polling when expired.**
     *
     * When you receive a `200` success response, save the `access_token` so your app can
     * authenticate the user in methods that require it. The `access_token` is valid for 3 months.
     * Save and use the `refresh_token` to get a new `access_token` without asking the user
     * to re-authenticate.
     * @param deviceCodeAccessTokenRequest Device Code
     */
    @POST("oauth/device/token")
    suspend fun exchangeDeviceCodeForAccessToken(
        @Body deviceCodeAccessTokenRequest: DeviceCodeAccessTokenRequest?
    ): AccessToken
}