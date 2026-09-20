package com.dira.app.guide

import com.dira.app.BuildConfig

object GuideClientFactory {
    fun resolvedBase(runtimeBase: String? = null): String {
        val runtime = runtimeBase?.trim().orEmpty()
        if (runtime.isNotEmpty()) return runtime.trimEnd('/')
        return BuildConfig.GUIDE_API_BASE.trim().trimEnd('/')
    }

    /** Mock when forced, or when no base URL is configured. */
    fun isMockMode(runtimeBase: String? = null): Boolean {
        if (BuildConfig.USE_MOCK_GUIDE) return true
        return resolvedBase(runtimeBase).isEmpty()
    }

    fun create(runtimeBase: String? = null): GuideApiClient {
        val base = resolvedBase(runtimeBase)
        return if (isMockMode(runtimeBase)) MockGuideClient() else HttpGuideClient(base)
    }
}
