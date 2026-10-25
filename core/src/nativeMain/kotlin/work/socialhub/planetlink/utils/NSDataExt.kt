package work.socialhub.planetlink.utils

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * NSData を Kotlin の ByteArray に変換する。
 * Swift の Data から画像などを渡すときに使う。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}

/**
 * Kotlin の ByteArray を NSData に変換する。
 * Swift の Data へ返すときに使う。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return NSData()
    }
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
