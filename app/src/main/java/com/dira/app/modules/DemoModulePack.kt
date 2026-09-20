package com.dira.app.modules

/**
 * Pluggable module pack rails — generic Android UI coach only.
 * No TRA / bank / PEPMIS / institution-specific playbooks.
 */
data class ModulePack(
    val id: String,
    val displayNameEn: String,
    val displayNameSw: String,
    val sensitivity: String,
    val commonTasks: List<String>,
)

object DemoModulePack {
    val pack = ModulePack(
        id = "generic.android",
        displayNameEn = "Generic Android UI coach",
        displayNameSw = "Mwongozo wa UI wa Android",
        sensitivity = "low",
        commonTasks = listOf(
            "Find the next button",
            "Open the overflow menu",
            "Go back",
        ),
    )
}
