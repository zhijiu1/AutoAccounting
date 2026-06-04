package org.ezbook.server.ai.providers

class DeepSeekProvider : BaseOpenAIProvider() {
    override val name: String = "deepseek"
    override val createKeyUri: String = "https://platform.deepseek.com/api-keys"
    override val apiUri: String = "https://api.deepseek.com"
    override var model: String = "deepseek-chat"
    override suspend fun getAvailableModels(): List<String> {
        return listOf("deepseek-chat","deepseek-reasoner")
    }
}