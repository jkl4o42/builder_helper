package git.jkl4o4.builder.sdk

import android.app.Activity
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppsFlyerHandler {

    private val keys = listOf(
        "campaign",
        "media_source",
        "af_channel",
        "af_status",
        "af_ad",
        "campaign_id",
        "adset_id",
        "adset",
        "ad_id",
    )

    suspend fun fetchAppsData(activity: Activity, appsKey: String): List<Pair<String, String?>> =
        suspendCancellableCoroutine { cancellableContinuation ->
            try {
                AppsFlyerLib.getInstance().init(appsKey, object : AppsFlyerConversionListener {
                    override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
                        if (cancellableContinuation.isActive) cancellableContinuation.resume(processData(p0))
                    }

                    override fun onConversionDataFail(p0: String?) {
                        if (cancellableContinuation.isActive) cancellableContinuation.resume(processData(null))
                    }

                    override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
                        if (cancellableContinuation.isActive) cancellableContinuation.resume(processData(null))
                    }

                    override fun onAttributionFailure(p0: String?) {
                        if (cancellableContinuation.isActive) cancellableContinuation.resume(processData(null))
                    }
                }, activity).start(activity)
            } catch (e: Exception) {
                if (cancellableContinuation.isActive) cancellableContinuation.resume(processData(null))
            }
        }

    private fun processData(data: MutableMap<String, Any>?): List<Pair<String, String?>>  {
        val listPairs: ArrayList<Pair<String, String?>> = arrayListOf()
        keys.forEach { key ->
            val encodeValue = data?.get(key).toString()
            listPairs.add(Pair(key, encodeValue))
        }
        return listPairs
    }
}