package git.jkl4o4.builder.utils

import git.jkl4o4.builder.Result

object Callbacks {
    var onSuccess: ((success: Result.Success) -> Unit)? = null
    var onError: ((error: Result.Error) -> Unit)? = null
}