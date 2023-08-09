package git.jkl4o4.builder

import android.app.Activity

interface Builder {

    suspend fun build(activity: Activity, domain: String, fbId: String, fbToken: String, fbKey: String): Result

    class TestSuccessResult: Builder {
        override suspend fun build(
            activity: Activity,
            domain: String,
            fbId: String,
            fbToken: String,
            fbKey: String
        ): Result {
            return Result.Success("url")
        }

    }

    class TestErrorResult: Builder {
        override suspend fun build(
            activity: Activity,
            domain: String,
            fbId: String,
            fbToken: String,
            fbKey: String
        ): Result {
            return Result.Error(0, "Bad domain, please check what you send.")
        }
    }
}