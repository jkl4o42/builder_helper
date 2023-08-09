package git.jkl4o4.builder.sdk

import android.app.Activity
import android.content.Context
import android.os.BatteryManager
import android.provider.Settings
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import git.jkl4o4.builder.utils.AfUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DeviceHandler {
    suspend fun fetchDeviseData(activity: Activity, appsKey: String, fbId: String, fbToken: String, fbKey: String): List<Pair<String, String?>> = suspendCancellableCoroutine { cancellableContinuation ->
        CoroutineScope(Dispatchers.IO).launch {
            val batteryManager = activity.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryStatus: Float = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it != -1 }?.toFloat() ?: 100.0f
            val googleAdId: String? = try { AdvertisingIdClient.getAdvertisingIdInfo(activity).id } catch (e: Exception) { null }
            val adbEnabled: Boolean = Settings.Global.getInt(activity.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

            if (cancellableContinuation.isActive) cancellableContinuation.resume(
                arrayListOf(
                    Pair("google_adid", googleAdId),
                    Pair("af_userid", AfUserId.getAfUserId(activity)),
                    Pair("adb", adbEnabled.toString()),
                    Pair("battery", batteryStatus.toString()),
                    Pair("bundle", activity.packageName),
                    Pair("dev_key", appsKey),
                    Pair("fb_app_id", fbId),
                    Pair("fb_at", fbToken),
                    Pair("account_id", getAccountId(activity, fbKey)),
                )
            )
        }
    }

    private suspend fun getAccountId(activity: Activity, fbKey: String): String?= suspendCoroutine { continuation ->
        val referrerClient = InstallReferrerClient.newBuilder(activity).build()
        try {
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(statusCode: Int) {
                    when (statusCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val referrerInfo = referrerClient.installReferrer
                            continuation.resume(parseReferrer(referrerInfo.installReferrer, fbKey))
                        }
                        else -> continuation.resume(null)
                    }
                    referrerClient.endConnection()
                }
                override fun onInstallReferrerServiceDisconnected() {
                    continuation.resume(null)
                }
            })
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    fun parseReferrer(referral: String?, cipherKey: String): String? {
        val referralDecoded = safelyDecodeReferral(referral) ?: return null
        if (!referralDecoded.contains("utm_content")) return null
        return tryToDecryptReferrer(referralDecoded, cipherKey)
    }

    private fun safelyDecodeReferral(referral: String?): String? {
        return try {
            URLDecoder.decode(referral, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            null
        }
    }

    private fun tryToDecryptReferrer(referralDecoded: String, cipherKey: String): String? {
        return try {
            decryptReferrer(referralDecoded, cipherKey)
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptReferrer(referralDecoded: String, cipherKey: String): String {
        val contentData = referralDecoded.split("${"utm_content"}=")[1]
        val parsedJson = JSONObject(contentData)
        val sourceData = JSONObject(parsedJson["source"].toString())
        val decryptedContent = performDecryption(sourceData, cipherKey)
        return decryptedContent["account_id"].toString()
    }

    private fun performDecryption(sourceData: JSONObject, cipherKey: String): JSONObject {
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(hexToBytes(cipherKey), "AES/GCM/NoPadding"),
            IvParameterSpec(hexToBytes(sourceData["nonce"].toString()))
        )
        val decryptedContentBytes = decryptCipher.doFinal(hexToBytes(sourceData["data"].toString()))
        return JSONObject(String(decryptedContentBytes))
    }

    private fun hexToBytes(hexString: String): ByteArray {
        return hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}