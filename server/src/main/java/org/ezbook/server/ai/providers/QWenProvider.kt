package org.ezbook.server.ai.providers

class QWenProvider : BaseOpenAIProvider() {
    override val name: String = "qwen"
    override val createKeyUri: String = "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key"
    override val apiUri: String = "https://dashscope.aliyuncs.com/compatible-mode"
    override var model: String = "qwen-turbo"
    override suspend fun getAvailableModels(): List<String> {
        return listOf(model,"qwen-plus","qwen-max","qwen-max-latest","qwen-long","qwen-plus-long","qwen-vl-max","qwen-vl-plus")
    }
}