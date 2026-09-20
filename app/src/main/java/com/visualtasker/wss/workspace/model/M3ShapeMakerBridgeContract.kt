package com.visualtasker.wss.workspace.model

object M3ShapeMakerBridgeContract {
    const val PACKAGE_NAME = "com.m3shapes.editor"
    const val ACTION_CREATE_VISUAL_ASSET = "com.m3shapes.editor.action.CREATE_VISUAL_ASSET"
    const val EXTRA_REQUESTED_ASSET_TYPE = "com.m3shapes.editor.extra.REQUESTED_ASSET_TYPE"
    const val EXTRA_RESULT_ASSET_ID = "com.m3shapes.editor.extra.RESULT_ASSET_ID"
}

data class VisualAssetCapabilitySnapshot(
    val embeddedDesigner: Boolean,
    val externalEditor: Boolean,
    val emaImport: Boolean,
    val emaExport: Boolean,
    val assetRuntime: Boolean,
)

object VisualAssetCapabilityResolver {
    fun resolve(externalEditorAvailable: Boolean): VisualAssetCapabilitySnapshot =
        VisualAssetCapabilitySnapshot(
            embeddedDesigner = true,
            externalEditor = externalEditorAvailable,
            emaImport = true,
            emaExport = true,
            assetRuntime = true,
        )
}
