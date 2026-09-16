package com.dira.app.guide

import com.dira.app.BuildConfig

object GuideClientFactory {
    fun create(): GuideApiClient {
        val base = BuildConfig.GUIDE_API_BASE.trim()
        val useMock = BuildConfig.USE_MOCK_GUIDE || base.isEmpty()
        return if (useMock) MockGuideClient() else HttpGuideClient(base)
    }

    fun isMockMode(): Boolean {
        val base = BuildConfig.GUIDE_API_BASE.trim()
        return BuildConfig.USE_MOCK_GUIDE || base.isEmpty()
    }
}
