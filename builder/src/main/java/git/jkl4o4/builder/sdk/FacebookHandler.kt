package git.jkl4o4.builder.sdk

import android.app.Activity
import com.facebook.FacebookSdk
import com.facebook.applinks.AppLinkData
import git.jkl4o4.builder.Result
import git.jkl4o4.builder.utils.Callbacks
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
class FacebookHandler {

    suspend fun fetchDeepLink(activity: Activity, fbId: String, fbToken: String): String? = suspendCancellableCoroutine { cancellableContinuation ->
        FacebookSdk.apply {
            setApplicationId(fbId)
            setClientToken(fbToken)
            sdkInitialize(activity)
            setAdvertiserIDCollectionEnabled(true)
            setAutoInitEnabled(true)
            fullyInitialize()
        }
        try {
            AppLinkData.fetchDeferredAppLinkData(activity) { appLinkData ->
                try {
                    val deepLink = appLinkData?.targetUri?.toString()
                    if (cancellableContinuation.isActive) cancellableContinuation.resume(deepLink)
                } catch (e: Exception) {
                    Callbacks.onError?.invoke(Result.Error(e))
                    if (cancellableContinuation.isActive)cancellableContinuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Callbacks.onError?.invoke(Result.Error(e))
            if (cancellableContinuation.isActive) cancellableContinuation.resume(null)
        }
    }
}