package git.jkl4o4.builder.utils

object Extensions {
    private fun String.encryptCaesar(offset: Int): String {
        return buildString {
            for (char in this@encryptCaesar) {
                val encryptedChar = when {
                    char.isLetter() -> {
                        val base = if (char.isLowerCase()) 'a' else 'A'
                        ((char - base + offset) % 26 + base.code).toChar()
                    }
                    else -> char
                }
                append(encryptedChar)
            }
        }
    }

    fun String.decryptCaesar(offset: Int): String {
        return this.encryptCaesar(-offset)
    }
}