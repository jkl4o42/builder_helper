package git.jkl4o4.builder

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.onesignal.OneSignal
import git.jkl4o4.builder.sdk.AppsFlyerHandler
import git.jkl4o4.builder.sdk.DeviceHandler
import git.jkl4o4.builder.sdk.FacebookHandler
import git.jkl4o4.builder.utils.AfUserId
import git.jkl4o4.builder.utils.Callbacks
import git.jkl4o4.builder.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface Builder {

    fun buildCallback(
        onSuccess: (success: Result.Success) -> Unit,
        onError: (error: Result.Error) -> Unit
    )

    class Base : Builder {
        private lateinit var activity: Activity
        private lateinit var domain: String
        private lateinit var fbId: String
        private lateinit var fbToken: String
        private lateinit var fbKey: String
        private lateinit var appsKey: String
        private lateinit var sub10: String

        fun activity(activity: Activity) = apply { this.activity = activity }
        fun domain(domain: String) = apply { this.domain = domain }
        fun fbId(fbId: String) = apply { this.fbId = fbId }
        fun fbToken(fbToken: String) = apply { this.fbToken = fbToken }
        fun fbKey(fbKey: String) = apply { this.fbKey = fbKey }
        fun appsKey(appsKey: String) = apply { this.appsKey = appsKey }
        fun sub10(sub10: String) = apply { this.sub10 = sub10 }

       private suspend fun build(): Result =
            suspendCancellableCoroutine { cancellableContinuation ->
                CoroutineScope(Dispatchers.IO).launch {
                    val appsData = AppsFlyerHandler().fetchAppsData(activity, appsKey)
                    Log.e("TAG","appsData: $appsData")
                    val deepLink = FacebookHandler().fetchDeepLink(activity, fbId, fbToken)
                    Log.e("TAG","deepLink: $deepLink")
                    val deviceData = DeviceHandler().fetchDeviseData(activity, appsKey, fbId, fbToken, fbKey)
                    Log.e("TAG","deviceData: $deviceData")
                    val campaign = deepLink ?: appsData.find { it.first == Constants.CAMPAIGN }?.second
                    Log.e("TAG","campaign: $campaign")
                    val subs = processCampaign(campaign)
                    Log.e("TAG","campaignData: $subs")

                    val urlBuilder = Uri.Builder()
                    urlBuilder.scheme(Constants.HTTPS)
                    urlBuilder.authority(domain)

                    appsData.forEach {
                        if (it.first == Constants.CAMPAIGN) urlBuilder.appendQueryParameter(it.first, campaign)
                        else urlBuilder.appendQueryParameter(it.first, it.second)
                    }

                    deviceData.forEach {
                        urlBuilder.appendQueryParameter(it.first, it.second)
                    }
                    val push = subs[1].takeIf { !it.isNullOrEmpty() }
                    Log.e("TAG","push: $push")
                    subs[9] = sub10
                    subs.forEachIndexed { index, value ->
                        urlBuilder.appendQueryParameter(
                            "${Constants.SUB}${index + 1}",
                            value
                        )
                    }
                    urlBuilder.appendQueryParameter(Constants.PUSH, push)
                    OneSignal.setExternalUserId(AfUserId.getAfUserId(activity).toString())
                    OneSignal.sendTag(Constants.SUB_APP, push ?: Constants.ORGANIC)
                    val buildUrl = replace(urlBuilder.toString(), Uri.encode(campaign.toString(), "utf-8"))
                    cancellableContinuation.resume(Result.Success(buildUrl))
                }
            }

        private fun replace(url: String, value: String): String {
            val regex = Regex("""([?&])${Constants.CAMPAIGN}=[^&]*""")
            return regex.replace(url, "$1${Constants.CAMPAIGN}=$value")
        }

        private fun processCampaign(inputStr: String?): ArrayList<String?> {
            val defaultValues = arrayListOf<String?>()
            defaultValues.add(null)
            repeat(10) { defaultValues.add("") }

            if (inputStr.isNullOrEmpty() || inputStr == Constants.NONE) {
                return defaultValues
            }

            val elements =  if (inputStr.contains("/")) {
                inputStr.split("/").last().split("_")
            } else inputStr.split("_")
            val result = ArrayList<String?>(11)
            result.addAll(elements)
            repeat(11 - elements.size) { result.add("") }
            return result
        }

        override fun buildCallback(
            onSuccess: (success: Result.Success) -> Unit,
            onError: (error: Result.Error) -> Unit
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                Callbacks.onSuccess = onSuccess
                Callbacks.onError = onError
                val result = build()
                when (result::class) {
                    Result.Success::class -> {
                        Callbacks.onSuccess?.invoke(result as Result.Success)
                    }
                    Result.Error::class ->  {
                        Callbacks.onError?.invoke(result as Result.Error)
                    }
                }
            }
        }
    }
}