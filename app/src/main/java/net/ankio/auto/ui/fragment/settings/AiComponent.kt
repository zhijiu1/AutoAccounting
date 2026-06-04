/*
 * Copyright (C) 2025 ankio

 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ankio.auto.ui.fragment.settings

import android.view.View
import android.text.InputType
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.databinding.ComponentAiBinding
import net.ankio.auto.http.api.AiAPI
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.utils.PrefManager

/**
 * AI settings component
 */
class AiComponent(
    binding: ComponentAiBinding
) : BaseComponent<ComponentAiBinding>(binding) {

    private var providerList: List<String> = emptyList()
    private var createKeyUri = ""
    private var models: List<String> = emptyList()

    override fun onComponentCreate() {
        super.onComponentCreate()
        bindListeners()
        if (BuildConfig.DEBUG) {
            binding.etAiToken.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    private suspend fun loadProvider(provider: String) {
        runCatching { AiAPI.getInfo(provider) }.onSuccess { info ->
            val apiUri = info["apiUri"].orEmpty()
            val apiModel = info["apiModel"].orEmpty()
            createKeyUri = info["createKeyUri"].orEmpty()
            if (apiUri.isNotEmpty()) binding.etAiBaseUrl.setText(apiUri)
            if (apiModel.isNotEmpty()) binding.actAiModel.setText(apiModel, false)
            binding.etAiToken.setText("")
        }
    }

    /**
     * Resolve the provider name from UI text, matching against the known provider list
     * to guard against whitespace/encoding mismatches that would cause the backend
     * to silently fall back to DeepSeek.
     */
    private fun resolveProvider(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        // Exact match first (most common case)
        if (providerList.any { it == trimmed }) return trimmed
        // Case-insensitive fallback
        providerList.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }
        // Whitespace-insensitive fallback
        providerList.firstOrNull { it.trim() == trimmed }?.let { return it }
        return trimmed
    }

    private fun bindListeners() = with(binding) {
        // FIX: Save provider to PrefManager on selection
        actAiProvider.setOnItemClickListener { _, _, pos, _ ->
            actAiModel.setText("")
            tilAiToken.error = null
            val provider = providerList.getOrNull(pos).orEmpty()
            if (provider.isNotEmpty()) {
                PrefManager.apiProvider = provider
            }
            launch {
                loadProvider(provider)
            }
        }

        btnRefreshModels.setOnClickListener { fetchModels() }

        // FIX: Use adapter.getItem for correct filtered position, persist model
        actAiModel.setOnItemClickListener { parent, _, pos, _ ->
            val model = parent.adapter.getItem(pos)?.toString().orEmpty()
            if (model.isNotEmpty()) {
                actAiModel.setText(model, false)
                PrefManager.apiModel = model
            }
        }

        btnGetToken.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(createKeyUri)
        }

        btnTestAi.setOnClickListener { testAiConnection() }
    }

    private fun showTestResult(isSuccess: Boolean, message: String, icon: Int) = with(binding) {
        cardTestResult.visibility = View.VISIBLE
        tvTestResultTitle.apply {
            setText(context.getString(R.string.ai_test_result))
            setColor(
                if (isSuccess) DynamicColors.Primary else DynamicColors.Error
            )
            setIcon(context.getDrawable(icon), true)
        }
        tvTestResultContent.text = message
    }

    private fun testAiConnection() = with(binding) {
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        val provider = resolveProvider(actAiProvider.text?.toString().orEmpty())
        val model = actAiModel.text?.toString()?.trim().orEmpty()
        val apiUri = etAiBaseUrl.text?.toString()?.trim().orEmpty()

        if (token.isBlank() || provider.isBlank() || model.isBlank()) {
            showTestResult(
                false,
                context.getString(R.string.ai_test_no_config),
                R.drawable.ic_error
            )
            return@with
        }

        val loading = LoadingUtils(context)
        launch {
            try {
                loading.show()
                val result = AiAPI.request(
                    systemPrompt = "You are a helpful assistant. Please respond briefly.",
                    userPrompt = "Hello, please respond with a simple greeting to confirm the connection is working.",
                    provider = provider,
                    apiKey = token,
                    apiUri = apiUri,
                    model = model
                )

                if (result.isSuccess) {
                    val response = result.getOrNull().orEmpty()
                    showTestResult(
                        true,
                        context.getString(R.string.ai_test_success_message) + "\n\n" +
                                " ${response.take(100)}${if (response.length > 100) "..." else ""}",
                        R.drawable.ic_success
                    )
                    PrefManager.apply {
                        apiProvider = provider
                        apiKey = token
                        this.apiUri = apiUri
                        apiModel = model
                        featureAiAvailable = true
                    }
                } else {
                    PrefManager.featureAiAvailable = false
                    val errMsg = result.exceptionOrNull()?.message ?: "Empty response"
                    showTestResult(
                        false,
                        context.getString(R.string.ai_test_failed_message, errMsg),
                        R.drawable.ic_error
                    )
                }
            } finally {
                loading.close()
            }
        }
    }

    // FIX: Add error handling for fetchModels + resolve provider to known list
    private fun fetchModels() = with(binding) {
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            tilAiToken.error = context.getString(R.string.error_token_required)
            return@with
        }

        val loading = LoadingUtils(context)
        launch {
            try {
                loading.show()
                val provider = resolveProvider(actAiProvider.text?.toString().orEmpty())
                val url = etAiBaseUrl.text?.toString()?.trim().orEmpty()
                models = AiAPI.getModels(provider = provider, apiKey = token, apiUri = url)

                if (models.isEmpty()) {
                    tilAiToken.error = "Failed to load models. Check API Key."
                } else {
                    actAiModel.setSimpleItems(models.toTypedArray())
                    tilAiToken.error = null
                }
            } catch (e: Exception) {
                tilAiToken.error = "Error: ${e.message}"
            } finally {
                loading.close()
            }
        }
    }

    override fun onComponentResume() {
        super.onComponentResume()
        launch {
            providerList = AiAPI.getProviders()
            binding.actAiProvider.setSimpleItems(providerList.toTypedArray())
            loadProvider(PrefManager.apiProvider)
            binding.actAiProvider.setText(PrefManager.apiProvider, false)
            binding.etAiToken.setText(PrefManager.apiKey)
            binding.etAiBaseUrl.setText(PrefManager.apiUri)
            binding.actAiModel.setText(PrefManager.apiModel, false)
        }
    }

}
