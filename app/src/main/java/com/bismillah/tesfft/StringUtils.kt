package com.bismillah.tesfft

object StringUtils {
    // Hitung Levenshtein distance
    fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + if (a[i-1] == b[j-1]) 0 else 1
                )
            }
        }
        return dp[a.length][b.length]
    }
    // Hitung similarity % (0..100)
    fun similarity(a: String, b: String): Int {
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return if (maxLen == 0) 100 else ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
    }
}
