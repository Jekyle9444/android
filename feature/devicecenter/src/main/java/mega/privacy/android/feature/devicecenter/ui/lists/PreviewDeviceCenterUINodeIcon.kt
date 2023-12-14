package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A Preview Composable that displays all possible Device and Device Folder Icons
 *
 * @param icon The [DeviceCenterUINodeIcon] generated by the [PreviewDeviceCenterUINodeIcon]
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterUINodeIcon(
    @PreviewParameter(DeviceCenterUINodeIconProvider::class) icon: DeviceCenterUINodeIcon,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Backup Name",
                icon = icon,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
            onDeviceClicked = {},
            onNonBackupFolderMenuClicked = {},
        )
    }
}

/**
 * A [PreviewParameterProvider] class that provides the list of Device and Device Folder Icons to
 * be displayed in the Composable preview
 */
private class DeviceCenterUINodeIconProvider :
    PreviewParameterProvider<DeviceCenterUINodeIcon> {
    override val values = listOf(
        DeviceIconType.Android,
        DeviceIconType.IOS,
        DeviceIconType.Linux,
        DeviceIconType.Mac,
        DeviceIconType.Mobile,
        DeviceIconType.PC,
        DeviceIconType.Windows,
        FolderIconType.Backup,
        FolderIconType.CameraUploads,
        FolderIconType.Folder,
        FolderIconType.Sync,
    ).asSequence()
}