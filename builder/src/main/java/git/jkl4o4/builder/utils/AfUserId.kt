package git.jkl4o4.builder.utils

import android.app.Activity
import com.appsflyer.AppsFlyerLib

object AfUserId {
    fun getAfUserId(activity: Activity): String? {
       return try { AppsFlyerLib.getInstance().getAppsFlyerUID(activity) } catch (e: Exception) { null }
    }
}