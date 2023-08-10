package git.jkl4o4.builder.sdk

import android.app.Activity
import android.content.Context
import android.os.BatteryManager
import android.provider.Settings
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import git.jkl4o4.builder.Result
import git.jkl4o4.builder.utils.AfUserId
import git.jkl4o4.builder.utils.Callbacks
import git.jkl4o4.builder.utils.Constants
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
            val googleAdId: String? = try { AdvertisingIdClient.getAdvertisingIdInfo(activity).id } catch (e: Exception) {
                Callbacks.onError?.invoke(Result.Error(e))
                null
            }
            val adbEnabled: Boolean = Settings.Global.getInt(activity.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

            if (cancellableContinuation.isActive) cancellableContinuation.resume(
                arrayListOf(
                    Pair(Constants.GOOGLE_ADID, googleAdId),
                    Pair(Constants.AF_USERID, AfUserId.getAfUserId(activity)),
                    Pair(Constants.ADB, adbEnabled.toString()),
                    Pair(Constants.BATTERY, batteryStatus.toString()),
                    Pair(Constants.BUNDLE, activity.packageName),
                    Pair(Constants.DEV_KEY, appsKey),
                    Pair(Constants.FB_APP_ID, fbId),
                    Pair(Constants.FB_AT, fbToken),
                    Pair(Constants.ACCOUNT_ID, getAccountId(activity, fbKey)),
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
            Callbacks.onError?.invoke(Result.Error(e))
            continuation.resume(null)
        }
    }

    fun parseReferrer(referral: String?, cipherKey: String): String? {
        val referralDecoded = safelyDecodeReferral(referral) ?: return null
        if (!referralDecoded.contains(Constants.UTM_CONTENT)) return null
        return tryToDecryptReferrer(referralDecoded, cipherKey)
    }

    private fun safelyDecodeReferral(referral: String?): String? {
        return try {
            URLDecoder.decode(referral, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            Callbacks.onError?.invoke(Result.Error(e))
            null
        }
    }

    private fun tryToDecryptReferrer(referralDecoded: String, cipherKey: String): String? {
        return try {
            decryptReferrer(referralDecoded, cipherKey)
        } catch (e: Exception) {
            Callbacks.onError?.invoke(Result.Error(e))
            null
        }
    }

    private fun decryptReferrer(referralDecoded: String, cipherKey: String): String {
        val contentData = referralDecoded.split("${Constants.UTM_CONTENT}=")[1]
        val parsedJson = JSONObject(contentData)
        val sourceData = JSONObject(parsedJson[Constants.SOURCE].toString())
        val decryptedContent = performDecryption(sourceData, cipherKey)
        return decryptedContent[Constants.ACCOUNT_ID].toString()
    }

    private fun performDecryption(sourceData: JSONObject, cipherKey: String): JSONObject {
        val decryptCipher = Cipher.getInstance(Constants.ALGORITHM)
        decryptCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(hexToBytes(cipherKey), Constants.ALGORITHM),
            IvParameterSpec(hexToBytes(sourceData[Constants.NONCE].toString()))
        )
        val decryptedContentBytes = decryptCipher.doFinal(hexToBytes(sourceData[Constants.DATA].toString()))
        return JSONObject(String(decryptedContentBytes))
    }

    private fun hexToBytes(hexString: String): ByteArray {
        return hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}