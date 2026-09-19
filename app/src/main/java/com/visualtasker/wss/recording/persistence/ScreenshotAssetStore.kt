package com.visualtasker.wss.recording.persistence

import com.visualtasker.wss.recording.ScreenshotAsset
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class StoredScreenshotAsset(
    val asset: ScreenshotAsset,
    val created: Boolean,
    val file: File,
)

class ScreenshotAssetStore(
    private val rootDirectory: File,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    fun putPng(bytes: ByteArray): StoredScreenshotAsset {
        require(bytes.hasPngSignature()) { "Screenshot payload is not a PNG." }
        val hash = bytes.sha256()
        val relative = "screenshots/${hash.take(2)}/$hash.png"
        val target = File(rootDirectory, relative)
        if (target.isFile) {
            check(target.readBytes().sha256() == hash) { "Existing screenshot asset hash mismatch: $hash" }
            return StoredScreenshotAsset(
                ScreenshotAsset(hash, relative, target.length(), target.lastModified()),
                created = false,
                file = target,
            )
        }

        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            check(temporary.readBytes().sha256() == hash) { "Temporary screenshot asset hash mismatch: $hash" }
            check(temporary.renameTo(target) || atomicFallback(temporary, target)) {
                "Could not finalize screenshot asset: ${target.absolutePath}"
            }
            return StoredScreenshotAsset(
                ScreenshotAsset(hash, relative, bytes.size.toLong(), nowEpochMs()),
                created = true,
                file = target,
            )
        } finally {
            temporary.delete()
        }
    }

    fun resolve(reference: String): File? =
        File(rootDirectory, reference).takeIf { candidate ->
            candidate.canonicalPath.startsWith(rootDirectory.canonicalPath + File.separator) && candidate.isFile
        }

    fun deleteIfCreated(stored: StoredScreenshotAsset) {
        if (stored.created) stored.file.delete()
    }

    private fun atomicFallback(source: File, target: File): Boolean = runCatching {
        FileOutputStream(target).use { output ->
            source.inputStream().use { it.copyTo(output) }
            output.fd.sync()
        }
        target.readBytes().sha256() == source.readBytes().sha256()
    }.getOrDefault(false)
}

private fun ByteArray.hasPngSignature(): Boolean =
    size >= 8 &&
        this[0] == 0x89.toByte() && this[1] == 0x50.toByte() && this[2] == 0x4E.toByte() &&
        this[3] == 0x47.toByte() && this[4] == 0x0D.toByte() && this[5] == 0x0A.toByte() &&
        this[6] == 0x1A.toByte() && this[7] == 0x0A.toByte()

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
