package org.ezbook.server.ai.providers

class KimiProvider : BaseOpenAIProvider() {
    override val name: String = "kimi"
    override val createKeyUri: String = "https://platform.moonshot.cn/console/api-keys"
    override val apiUri: String = "https://api.moonshot.cn"
    override var model: String = "moonshot-v1-128k"
    override suspend fun getAvailableModels(): List<String> {
        return listOf("moonshot-v1-128k","moonshot-v1-32k","moonshot-v1-8k")
    }
}