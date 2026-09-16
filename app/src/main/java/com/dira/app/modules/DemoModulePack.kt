package com.dira.app.modules

/**
 * Pluggable module pack rails — demo only in Phase 2.
 * No hardcoded TRA/bank production modules.
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
        id = "demo.ui",
        displayNameEn = "Demo module (no real TRA/bank yet)",
        displayNameSw = "Moduli ya onyesho (bado hakuna TRA/benki)",
        sensitivity = "low",
        commonTasks = listOf("Find the action button", "Submit the form", "Finish the task"),
    )
}
