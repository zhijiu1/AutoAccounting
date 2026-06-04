package org.ezbook.server.ai.providers

class ChatGPTProvider : BaseOpenAIProvider() {
    override val name: String = "chatgpt"
    override val createKeyUri: String = "https://platform.openai.com/api-keys"
    override val apiUri: String = "https://api.openai.com"
    override var model: String = "gpt-4o"
    override suspend fun getAvailableModels(): List<String> {
        return listOf("gpt-4o","gpt-4o-mini","gpt-4.1","gpt-4.1-mini","o4-mini","gpt-3.5-turbo")
    }
}