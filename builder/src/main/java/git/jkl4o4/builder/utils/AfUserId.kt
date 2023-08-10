package git.jkl4o4.builder.utils

import android.app.Activity
import com.appsflyer.AppsFlyerLib
import git.jkl4o4.builder.Result

object AfUserId {
    fun getAfUserId(activity: Activity): String? {
       return try { AppsFlyerLib.getInstance().getAppsFlyerUID(activity) } catch (e: Exception) {
           Callbacks.onError?.invoke(Result.Error(e))
           null
       }
    }
}