package com.dira.app.guide

/** HTTPS guide brain contract — mock or real HTTP. */
interface GuideApiClient {
    suspend fun requestGuidance(request: GuideRequest): GuideResponse
}
