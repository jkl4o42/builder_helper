package git.jkl4o4.builder

sealed class Result {
    data class Success(val url: String) : Result()
    data class Error(val exception: Exception) : Result()
}