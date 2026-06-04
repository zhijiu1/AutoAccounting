package org.ezbook.server.ai.providers

class MiMoProvider : BaseOpenAIProvider() {
    override val name: String = "mimo"
    override val createKeyUri: String = "https://platform.xiaomimimo.com/#/console/api-keys"
    override val apiUri: String = "https://token-plan-cn.xiaomimimo.com"
    override var model: String = "mimo-v2.5"
    override suspend fun getAvailableModels(): List<String> {
        return listOf("mimo-v2.5", "mimo-v2-flash")
    }
}