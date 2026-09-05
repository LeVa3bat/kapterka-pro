fun computeKeyChecksum(p1: String, p2: String): String {
    val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    val s = "KAPT-$p1-$p2-KAPT3RKA_881_MILITARY"
    var h1 = 0x811c9dc5L
    var h2 = 0x5a2d1e39L
    for (ch in s) {
        val code = ch.code.toLong()
        h1 = ((h1 xor code) * 0x01000193L) and 0xFFFFFFFFL
        h2 = (((h2 + code) * 31L) + 0x45L) and 0xFFFFFFFFL
    }
    val c0 = chars[((h1 ushr 24) and 0x1FL).toInt()]
    val c1 = chars[((h1 ushr 16) and 0x1FL).toInt()]
    val c2 = chars[((h2 ushr 24) and 0x1FL).toInt()]
    val c3 = chars[((h2 ushr 16) and 0x1FL).toInt()]
    return "$c0$c1$c2$c3"
}
fun main() {
    println(computeKeyChecksum("ABCD", "EFGH"))
}
