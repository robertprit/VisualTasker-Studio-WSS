package com.visualtasker.wss.emscript.runtime

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

interface VariableValueStore {
    fun read(namespace: String, variableId: String): VariableReadResult
    fun compareAndSet(
        namespace: String,
        variableId: String,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult
}

class AtomicFileVariableValueStore(private val root: File) : VariableValueStore {
    private val recoveryError: String?

    init {
        recoveryError = runCatching {
            check(root.isDirectory || root.mkdirs()) { "Could not create variable storage directory." }
            val recoveryFiles = root.walkTopDown().filter(File::isFile).sortedBy(File::getAbsolutePath).toList()
            recoveryFiles.filter { it.name.endsWith(BACKUP_SUFFIX) }.forEach(::recoverBackup)
            recoveryFiles.filter { it.name.endsWith(TEMP_SUFFIX) }.forEach(::recoverTemporary)
        }.exceptionOrNull()?.message
    }

    override fun read(namespace: String, variableId: String): VariableReadResult = synchronized(lock(namespace, variableId)) {
        recoveryError?.let { message -> return@synchronized storageFailure(variableId, "Variable recovery failed: $message") }
        val file = valueFile(namespace, variableId)
        if (!file.isFile) return@synchronized VariableReadResult.Missing
        runCatching { decode(file.readText(), namespace, variableId) }
            .fold(
                onSuccess = { stored -> VariableReadResult.Found(stored) },
                onFailure = { error ->
                    VariableReadResult.Failed(
                        VariableFailure(
                            VariableFailureCode.STORAGE_FAILURE,
                            "Stored variable '$variableId' cannot be decoded: ${error.message}",
                            variableId,
                        ),
                    )
                },
            )
    }

    override fun compareAndSet(
        namespace: String,
        variableId: String,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult = synchronized(lock(namespace, variableId)) {
        recoveryError?.let { message ->
            return@synchronized VariableWriteResult.Failed(storageFailureValue(variableId, "Variable recovery failed: $message"))
        }
        if (cancellationToken.isCancellationRequested) return@synchronized cancelled(variableId)
        val target = valueFile(namespace, variableId)
        val current = if (target.isFile) {
            runCatching { decode(target.readText(), namespace, variableId) }.getOrElse { error ->
                return@synchronized VariableWriteResult.Failed(
                    VariableFailure(VariableFailureCode.STORAGE_FAILURE, "Stored variable '$variableId' cannot be decoded: ${error.message}", variableId),
                )
            }
        } else {
            null
        }
        if (current?.revision != expectedRevision) {
            return@synchronized VariableWriteResult.Failed(
                VariableFailure(
                    VariableFailureCode.REVISION_CONFLICT,
                    "Variable '$variableId' expected revision $expectedRevision but found ${current?.revision}.",
                    variableId,
                ),
            )
        }
        val stored = VariableStoredValue(value, (current?.revision ?: -1L) + 1L)
        val temporary = File(target.parentFile, target.name + TEMP_SUFFIX)
        val backup = File(target.parentFile, target.name + BACKUP_SUFFIX)
        runCatching {
            target.parentFile?.mkdirs()
            FileOutputStream(temporary).use { output ->
                output.write(encode(namespace, variableId, stored).toByteArray(Charsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            if (cancellationToken.isCancellationRequested) {
                temporary.delete()
                return@synchronized cancelled(variableId)
            }
            backup.delete()
            if (target.exists() && !target.renameTo(backup)) error("Could not create variable backup")
            if (!temporary.renameTo(target)) {
                backup.renameTo(target)
                error("Could not atomically replace variable value")
            }
            backup.delete()
        }.getOrElse { error ->
            temporary.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            return@synchronized VariableWriteResult.Failed(
                VariableFailure(VariableFailureCode.STORAGE_FAILURE, "Variable '$variableId' write failed: ${error.message}", variableId),
            )
        }
        VariableWriteResult.Written(stored)
    }

    private fun valueFile(namespace: String, variableId: String): File =
        File(File(root, digest(namespace)), "${digest(variableId)}.json")

    private fun recoverBackup(backup: File) {
        if (!backup.exists()) return
        val target = File(backup.parentFile, backup.name.removeSuffix(BACKUP_SUFFIX))
        val temporary = File(backup.parentFile, target.name + TEMP_SUFFIX)
        if (target.exists()) {
            backup.delete()
        } else {
            check(backup.renameTo(target)) { "Could not recover variable backup '${backup.name}'." }
        }
        temporary.delete()
    }

    private fun recoverTemporary(temporary: File) {
        if (!temporary.exists()) return
        val target = File(temporary.parentFile, temporary.name.removeSuffix(TEMP_SUFFIX))
        if (target.exists()) {
            temporary.delete()
        } else {
            check(temporary.renameTo(target)) { "Could not recover variable temporary file '${temporary.name}'." }
        }
    }

    private fun lock(namespace: String, variableId: String): Any =
        locks.computeIfAbsent("$namespace\u0000$variableId") { Any() }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun encode(namespace: String, variableId: String, stored: VariableStoredValue): String = JSONObject()
        .put("schema", STORAGE_SCHEMA)
        .put("namespace", namespace)
        .put("variableId", variableId)
        .put("revision", stored.revision)
        .put("value", stored.value.toJson())
        .toString()

    private fun decode(raw: String, namespace: String, variableId: String): VariableStoredValue {
        val json = JSONObject(raw)
        require(json.getInt("schema") == STORAGE_SCHEMA) { "Unsupported variable storage schema" }
        require(json.getString("namespace") == namespace) { "Variable namespace mismatch" }
        require(json.getString("variableId") == variableId) { "Variable identity mismatch" }
        return VariableStoredValue(
            value = json.getJSONObject("value").toExecutionValue(),
            revision = json.getLong("revision"),
        )
    }

    private fun cancelled(variableId: String) = VariableWriteResult.Failed(
        VariableFailure(VariableFailureCode.CANCELLED, "Variable '$variableId' write was cancelled.", variableId),
    )

    private fun storageFailure(variableId: String, message: String) = VariableReadResult.Failed(storageFailureValue(variableId, message))

    private fun storageFailureValue(variableId: String, message: String) =
        VariableFailure(VariableFailureCode.STORAGE_FAILURE, message, variableId)

    private companion object {
        const val STORAGE_SCHEMA = 1
        const val TEMP_SUFFIX = ".tmp"
        const val BACKUP_SUFFIX = ".bak"
        val locks = ConcurrentHashMap<String, Any>()
    }
}

class ProjectVariableProvider(private val store: VariableValueStore) : VariableProvider {
    override val id: String = PROJECT_VARIABLE_PROVIDER_ID

    override fun read(binding: VariableBinding, cancellationToken: CancellationToken): VariableReadResult {
        if (cancellationToken.isCancellationRequested) return cancelledRead(binding)
        val projectId = binding.projectId ?: return contextFailure(binding, "Project scope requires a project ID.")
        return store.read("project:$projectId", binding.variableId)
    }

    override fun write(
        binding: VariableBinding,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult {
        val projectId = binding.projectId ?: return VariableWriteResult.Failed(contextFailureValue(binding, "Project scope requires a project ID."))
        return store.compareAndSet("project:$projectId", binding.variableId, value, expectedRevision, cancellationToken)
    }
}

class AppSharedVariableProvider(private val store: VariableValueStore) : VariableProvider {
    override val id: String = APP_SHARED_VARIABLE_PROVIDER_ID

    override fun read(binding: VariableBinding, cancellationToken: CancellationToken): VariableReadResult =
        if (cancellationToken.isCancellationRequested) cancelledRead(binding)
        else store.read(APP_SHARED_NAMESPACE, binding.variableId)

    override fun write(
        binding: VariableBinding,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult = store.compareAndSet(APP_SHARED_NAMESPACE, binding.variableId, value, expectedRevision, cancellationToken)

    private companion object { const val APP_SHARED_NAMESPACE = "app-shared" }
}

private fun contextFailure(binding: VariableBinding, message: String) = VariableReadResult.Failed(contextFailureValue(binding, message))

private fun contextFailureValue(binding: VariableBinding, message: String) = VariableFailure(
    VariableFailureCode.SCOPE_CONTEXT_MISSING,
    message,
    binding.variableId,
    binding.scope,
    binding.providerId,
)

private fun cancelledRead(binding: VariableBinding) = VariableReadResult.Failed(
    VariableFailure(VariableFailureCode.CANCELLED, "Variable read was cancelled.", binding.variableId, binding.scope, binding.providerId),
)

private fun ExecutionValue.toJson(): JSONObject = when (this) {
    is ExecutionValue.BooleanValue -> JSONObject().put("kind", "Bool").put("value", value)
    is ExecutionValue.NumberValue -> JSONObject().put("kind", "Number").put("value", value)
    is ExecutionValue.StringValue -> JSONObject().put("kind", "String").put("value", value)
    is ExecutionValue.ListValue -> JSONObject().put("kind", "List").put("value", JSONArray(values.map(ExecutionValue::toJson)))
    is ExecutionValue.PointValue -> JSONObject().put("kind", "Point").put("x", value.x).put("y", value.y)
    ExecutionValue.NullValue -> JSONObject().put("kind", "Null")
    ExecutionValue.VoidValue -> error("Void cannot be persisted")
    is ExecutionValue.PathSegmentValue -> JSONObject().put("kind", "PathSegment").put("value", value.toJson())
    is ExecutionValue.PathValue -> JSONObject().put("kind", "Path").put("value", value.toJson())
    is ExecutionValue.PointerPathValue -> JSONObject().put("kind", "PointerPath").put("value", value.toJson())
    is ExecutionValue.MultiPathValue -> JSONObject().put("kind", "MultiPath").put("value", JSONArray(value.pointers.map(PointerPath::toJson)))
}

private fun JSONObject.toExecutionValue(): ExecutionValue = when (getString("kind")) {
    "Bool" -> ExecutionValue.BooleanValue(getBoolean("value"))
    "Number" -> ExecutionValue.NumberValue(getDouble("value"))
    "String" -> ExecutionValue.StringValue(getString("value"))
    "List" -> getJSONArray("value").let { values ->
        ExecutionValue.ListValue((0 until values.length()).map { index -> values.getJSONObject(index).toExecutionValue() })
    }
    "Point" -> ExecutionValue.PointValue(PointerPathPoint(getDouble("x"), getDouble("y")))
    "PathSegment" -> ExecutionValue.PathSegmentValue(getJSONObject("value").toPointerPathSegment())
    "Path" -> ExecutionValue.PathValue(getJSONObject("value").toPointerPathGeometry())
    "PointerPath" -> ExecutionValue.PointerPathValue(getJSONObject("value").toPointerPath())
    "MultiPath" -> getJSONArray("value").let { pointers ->
        ExecutionValue.MultiPathValue(MultiPointerGesture((0 until pointers.length()).map { pointers.getJSONObject(it).toPointerPath() }))
    }
    "Null" -> ExecutionValue.NullValue
    else -> error("Unknown stored variable value kind '${getString("kind")}'")
}

private fun PointerPathPoint.toJson() = JSONObject().put("x", x).put("y", y)

private fun JSONObject.toPointerPathPoint() = PointerPathPoint(getDouble("x"), getDouble("y"))

private fun PointerPathSegment.toJson(): JSONObject = when (this) {
    is PointerPathSegment.Line -> JSONObject()
        .put("kind", "Line").put("from", from.toJson()).put("to", to.toJson())
    is PointerPathSegment.QuadraticBezier -> JSONObject()
        .put("kind", "QuadraticBezier").put("from", from.toJson()).put("control", control.toJson()).put("to", to.toJson())
    is PointerPathSegment.CubicBezier -> JSONObject()
        .put("kind", "CubicBezier").put("from", from.toJson()).put("control1", control1.toJson())
        .put("control2", control2.toJson()).put("to", to.toJson())
}

private fun JSONObject.toPointerPathSegment(): PointerPathSegment = when (getString("kind")) {
    "Line" -> PointerPathSegment.Line(getJSONObject("from").toPointerPathPoint(), getJSONObject("to").toPointerPathPoint())
    "QuadraticBezier" -> PointerPathSegment.QuadraticBezier(
        getJSONObject("from").toPointerPathPoint(),
        getJSONObject("control").toPointerPathPoint(),
        getJSONObject("to").toPointerPathPoint(),
    )
    "CubicBezier" -> PointerPathSegment.CubicBezier(
        getJSONObject("from").toPointerPathPoint(),
        getJSONObject("control1").toPointerPathPoint(),
        getJSONObject("control2").toPointerPathPoint(),
        getJSONObject("to").toPointerPathPoint(),
    )
    else -> error("Unknown PointerPath segment kind '${getString("kind")}'")
}

private fun PointerPathGeometry.toJson() = JSONObject()
    .put("startPoint", startPoint.toJson())
    .put("segments", JSONArray(segments.map(PointerPathSegment::toJson)))

private fun JSONObject.toPointerPathGeometry(): PointerPathGeometry {
    val segments = getJSONArray("segments")
    return PointerPathGeometry(
        getJSONObject("startPoint").toPointerPathPoint(),
        (0 until segments.length()).map { segments.getJSONObject(it).toPointerPathSegment() },
    )
}

private fun PointerPathTiming.toJson() = JSONObject()
    .put("durationMs", durationMs)
    .put("segmentDurationsMs", segmentDurationsMs?.let(::JSONArray))
    .put("speedPixelsPerSecond", speedPixelsPerSecond)
    .put("startHoldMs", startHoldMs)
    .put("endHoldMs", endHoldMs)
    .put("delayMs", delayMs)
    .put("repeat", repeat)

private fun JSONObject.toPointerPathTiming(): PointerPathTiming {
    val segmentDurations = optJSONArray("segmentDurationsMs")?.let { values ->
        (0 until values.length()).map(values::getLong)
    }
    return PointerPathTiming(
        durationMs = optLongOrNull("durationMs"),
        segmentDurationsMs = segmentDurations,
        speedPixelsPerSecond = optDoubleOrNull("speedPixelsPerSecond"),
        startHoldMs = getLong("startHoldMs"),
        endHoldMs = getLong("endHoldMs"),
        delayMs = getLong("delayMs"),
        repeat = getInt("repeat"),
    )
}

private fun PointerPath.toJson() = JSONObject()
    .put("pointerId", pointerId)
    .put("pointerIdExplicit", pointerIdExplicit)
    .put("geometry", geometry.toJson())
    .put("timing", timing.toJson())

private fun JSONObject.toPointerPath() = PointerPath(
    pointerId = getInt("pointerId"),
    pointerIdExplicit = getBoolean("pointerIdExplicit"),
    geometry = getJSONObject("geometry").toPointerPathGeometry(),
    timing = getJSONObject("timing").toPointerPathTiming(),
)

private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
