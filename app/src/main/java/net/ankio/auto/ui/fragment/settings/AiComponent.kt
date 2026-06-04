/*
 * Copyright (C) 2025 ankio

 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-3.0
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
 * AI 鎺ュ叆璁剧疆缁勪欢 - Linus寮忔瀬绠€璁捐
 *
 * 璁捐鍘熷垯锛? * 1. 娑堥櫎鏋勯€犲嚱鏁板弬鏁板啑浣?- 鍙渶瑕乥inding锛岃嚜鍔ㄦ帹鏂敓鍛藉懆鏈? * 2. 缁熶竴鍗忕▼绠＄悊 - 浣跨敤BaseComponent鐨刲aunch鏂规硶
 * 3. 瀹屾暣閿欒澶勭悊 - 鎵€鏈夌綉缁滆姹傞兘鏈夊紓甯告崟鑾? * 4. 绠€鍖栫姸鎬佺鐞?- 鍑忓皯涓嶅繀瑕佺殑鐘舵€佸彉閲? *
 * 鍔熻兘姒傝锛? * 1. 鏍规嵁鐢ㄦ埛杈撳叆鐨?Token 鍔ㄦ€佸惎鐢?Provider銆丮odel 涓嬫媺鍒楄〃
 * 2. 鏀寔鍒锋柊妯″瀷鍒楄〃銆佽烦杞埌妯″瀷 Key 鐢宠椤甸潰
 * 3. 鑷姩鐢熷懡鍛ㄦ湡绠＄悊锛屾棤闇€鎵嬪姩澶勭悊鍗忕▼娓呯悊
 */
class AiComponent(
    binding: ComponentAiBinding
) : BaseComponent<ComponentAiBinding>(binding) {

    // ------------------------------------ 鏈湴缂撳瓨鏁版嵁 ------------------------------------ //

    /** AI 鏈嶅姟鍟嗗垪琛?*/
    private var providerList: List<String> = emptyList()
    private var createKeyUri = ""
    /** AI 妯″瀷鍒楄〃 */
    private var models: List<String> = emptyList()

    // ------------------------------------ 鍒濆鍖?------------------------------------------ //

    override fun onComponentCreate() {
        super.onComponentCreate()
        bindListeners()
        // Debug 妯″紡涓嬪皢 Token 杈撳叆妗嗚缃负鏄庢枃鏂囨湰锛屼究浜庡紑鍙戣皟璇?        if (BuildConfig.DEBUG) {
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

    /** 缁熶竴娉ㄥ唽鎵€鏈?UI 浜嬩欢 */
    private fun bindListeners() = with(binding) {
        // Provider 閫夋嫨锛氭牴鎹悗绔俊鎭～鍏?URL / Model锛屽悓鏃朵繚瀛?provider 鍒版湰鍦?        actAiProvider.setOnItemClickListener { _, _, pos, _ ->
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

        // 鍒锋柊妯″瀷鍒楄〃
        btnRefreshModels.setOnClickListener { fetchModels() }


        // 閫変腑妯″瀷锛氭洿鏂?UI 骞舵寔涔呭寲淇濆瓨
        actAiModel.setOnItemClickListener { parent, _, pos, _ ->
            val model = parent.adapter.getItem(pos)?.toString().orEmpty()
            if (model.isNotEmpty()) {
                actAiModel.setText(model, false)
                PrefManager.apiModel = model
            }
        }

        // 璺宠浆娴忚鍣ㄦ垨澶嶅埗妯″瀷鐢宠鍦板潃
        btnGetToken.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(createKeyUri)
        }

        // AI 娴嬭瘯鍔熻兘
        btnTestAi.setOnClickListener { testAiConnection() }


    }

    // ------------------------------------ UI 鐘舵€佺鐞?------------------------------------ //

    // 绉婚櫎浜唘pdateTestButtonState鏂规硶 - 娴嬭瘯鎸夐挳濮嬬粓鍙敤

    /**
     * 鏄剧ず娴嬭瘯缁撴灉
     */
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

    // ------------------------------------ 缃戠粶浜や簰 ---------------------------------------- //

    /**
     * 娴嬭瘯 AI 杩炴帴 - Linus寮忕畝娲佹祴璇?     *
     * 璁捐鍘熷垯锛?     * 1. 绠€鍗曟祴璇?- 鍙戦€?Hello"楠岃瘉杩為€氭€?     * 2. 娓呮櫚鍙嶉 - 鎴愬姛/澶辫触鐘舵€佹槑纭?     * 3. 涓嶅奖鍝嶉厤缃?- 娴嬭瘯涓嶄慨鏀逛换浣曡缃?     * 4. 寮傚父瀹夊叏 - 鎵€鏈夐敊璇兘鏈夊鐞?     */
    private fun testAiConnection() = with(binding) {
        // 鍙傛暟楠岃瘉
        val token = etAiToken.text?.toString()?.trim().orEmpty()
        val provider = actAiProvider.text?.toString()?.trim().orEmpty()
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
                    // 娴嬭瘯鎴愬姛锛氫繚瀛樺埌鏈湴 Pref
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

    /**
     * 鐢ㄦ埛鐐瑰嚮"鍒锋柊妯″瀷"鏃惰皟鐢?- Linus寮忛敊璇鐞?     *
     * 璁捐鍘熷垯锛?     * 1. 鍙傛暟楠岃瘉鍦ㄦ渶鍓嶉潰 - 蹇€熷け璐?     * 2. 缁熶竴鐨勯敊璇鐞?- 涓嶈寮傚父娉勯湶鍒癠I灞?     * 3. 姝ｇ‘鐨勮祫婧愮鐞?- LoadingUtils浣跨敤context鑰屼笉鏄痑ctivity
     * 4. 娓呮櫚鐨勭姸鎬佸弽棣?- 鎴愬姛/澶辫触閮芥湁鏄庣‘鎻愮ず
     */
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
                val provider = actAiProvider.text?.toString()?.trim().orEmpty()
                val url = etAiBaseUrl.text?.toString()?.trim().orEmpty()
                models = AiAPI.getModels(provider = provider, apiKey = token, apiUri = url)

                // 鎴愬姛鏃舵洿鏂癠I
                actAiModel.setSimpleItems(models.toTypedArray())
                tilAiToken.error = null

            } catch (e: Exception) {
                tilAiToken.error = context.getString(R.string.ai_test_failed_message, e.message ?: "Unknown error")
            } finally {
                loading.close()
            }
        }
    }

    // ------------------------------------ 鐢熷懡鍛ㄦ湡 ---------------------------------------- //

    /** 鎭㈠椤甸潰鏃跺悓姝ュ悗绔姸鎬佸埌 UI - Linus寮忓紓甯稿畨鍏?*/
    override fun onComponentResume() {
        super.onComponentResume()
        launch {
            // 1) 杞藉叆 Provider 鍒楄〃
            providerList = AiAPI.getProviders()
            binding.actAiProvider.setSimpleItems(providerList.toTypedArray())
            loadProvider(PrefManager.apiProvider)
            // 2) 浣跨敤 PrefManager 灏嗛厤缃～鍏呭埌椤甸潰
            binding.actAiProvider.setText(PrefManager.apiProvider, false)
            binding.etAiToken.setText(PrefManager.apiKey)
            binding.etAiBaseUrl.setText(PrefManager.apiUri)
            binding.actAiModel.setText(PrefManager.apiModel, false)
        }
    }

}
