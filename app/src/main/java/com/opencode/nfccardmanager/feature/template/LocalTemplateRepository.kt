package com.opencode.nfccardmanager.feature.template

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object LocalTemplateRepository {
    private val defaultTemplates = listOf(
        WriteTemplate(
            id = "device_init",
            name = "设备初始化模板",
            version = "v1.0",
            content = "设备编号=EQ-001\n状态=启用\n类型=巡检设备",
            description = "用于设备首次发卡初始化",
        ),
        WriteTemplate(
            id = "asset_tag",
            name = "资产标签模板",
            version = "v1.0",
            content = "资产编码=ASSET-1001\n归属=仓储中心\n状态=在库",
            description = "用于固定资产标识",
        ),
        WriteTemplate(
            id = "visitor_card",
            name = "访客卡模板",
            version = "v1.0",
            content = "访客类型=临时\n权限=访客\n有效期=当天",
            description = "用于访客 NFC 标签初始化",
        ),
    )
    private var dbHelper: TemplateDbHelper? = null
    private val _templates = MutableStateFlow(
        defaultTemplates,
    )

    val templates: StateFlow<List<WriteTemplate>> = _templates.asStateFlow()

    fun init(context: Context) {
        if (dbHelper != null) return
        dbHelper = TemplateDbHelper(context.applicationContext)
        val current = dbHelper?.queryAll().orEmpty()
        if (current.isEmpty()) {
            defaultTemplates.forEach { template ->
                dbHelper?.insert(template)
            }
            _templates.value = defaultTemplates
        } else {
            _templates.value = current
        }
    }

    fun listTemplates(): List<WriteTemplate> = _templates.value

    fun findById(id: String): WriteTemplate? = _templates.value.firstOrNull { it.id == id }

    fun addTemplate(name: String, description: String, content: String) {
        val newId = buildString {
            append("template_")
            append(System.currentTimeMillis())
        }
        val newTemplate = WriteTemplate(
            id = newId,
            name = name,
            version = "v1.0",
            content = content,
            description = description,
        )
        dbHelper?.insert(newTemplate)
        _templates.update { templates -> templates + newTemplate }
    }

    fun updateTemplate(id: String, name: String, description: String, content: String) {
        _templates.update { templates ->
            templates.map { template ->
                if (template.id == id) {
                    template.copy(
                        name = name,
                        description = description,
                        content = content,
                        version = bumpVersion(template.version),
                    ).also { updated ->
                        dbHelper?.update(updated)
                    }
                } else {
                    template
                }
            }
        }
    }

    fun deleteTemplate(id: String) {
        dbHelper?.delete(id)
        _templates.update { templates ->
            templates.filterNot { it.id == id }
        }
    }

    private fun bumpVersion(version: String): String {
        val numeric = version.removePrefix("v").toDoubleOrNull() ?: return "v1.1"
        return "v${"%.1f".format(numeric + 0.1)}"
    }
}
