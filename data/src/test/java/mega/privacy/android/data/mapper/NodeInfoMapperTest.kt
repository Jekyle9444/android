package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.NodeFile
import mega.privacy.android.domain.entity.NodeFolder
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class NodeInfoMapperTest {
    private val expectedName = "testName"
    private val expectedSize = 1000L
    private val expectedLabel = MegaNode.NODE_LBL_RED
    private val expectedId = 1L
    private val expectedParentId = 2L
    private val expectedBase64Id = "1L"
    private val expectedModificationTime = 123L

    @Test
    fun `test that files are mapped if isFile is true`() = runTest {
        val megaNode = getMockNode(isFile = true)
        val actual =
            toNodeInfo(
                megaNode = megaNode,
                thumbnailPath = { null },
                hasVersion = { false },
                numberOfChildFolders = { 0 },
                numberOfChildFiles = { 1 },
                isInRubbish = { false },
                fileTypeInfoMapper = { PdfFileTypeInfo },
                isPendingShare = { false },
            )

        assertThat(actual).isInstanceOf(NodeFile::class.java)
    }

    @Test
    fun `test that folders are mapped if isFile is false`() = runTest {
        val megaNode = getMockNode(isFile = false)
        val actual =
            toNodeInfo(
                megaNode = megaNode,
                thumbnailPath = { null },
                hasVersion = { false },
                numberOfChildFolders = { 0 },
                numberOfChildFiles = { 1 },
                isInRubbish = { false },
                fileTypeInfoMapper = { PdfFileTypeInfo },
                isPendingShare = { false },
            )

        assertThat(actual).isInstanceOf(NodeFolder::class.java)
    }

    @Test
    fun `test that values returned by gateway are used`() = runTest {
        val node = getMockNode(isFile = false)
        val expectedHasVersion = true
        val expectedNumChildFolders = 2
        val expectedNumChildFiles = 3
        val gateway = mock<MegaApiGateway> {
            onBlocking { hasVersion(node) }.thenReturn(expectedHasVersion)
            onBlocking { getNumChildFolders(node) }.thenReturn(expectedNumChildFolders)
            onBlocking { getNumChildFiles(node) }.thenReturn(expectedNumChildFiles)
            onBlocking { isInRubbish(node) }.thenReturn(true)
            onBlocking { isPendingShare(node) }.thenReturn(true)
        }

        val actual = toNodeInfo(
            megaNode = node,
            thumbnailPath = { null },
            hasVersion = gateway::hasVersion,
            numberOfChildFolders = gateway::getNumChildFolders,
            numberOfChildFiles = gateway::getNumChildFiles,
            isInRubbish = gateway::isInRubbish,
            fileTypeInfoMapper = { PdfFileTypeInfo },
            isPendingShare = gateway::isPendingShare,
        )

        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.label).isEqualTo(expectedLabel)
        assertThat(actual.hasVersion).isEqualTo(expectedHasVersion)
        assertThat(actual.id).isEqualTo(expectedId)
        assertThat(actual.parentId).isEqualTo(expectedParentId)
        assertThat(actual.base64Id).isEqualTo(expectedBase64Id)
        assertThat(actual.isFavourite).isEqualTo(node.isFavourite)
        assertThat(actual.isExported).isEqualTo(node.isExported)
        assertThat(actual.isTakenDown).isEqualTo(node.isTakenDown)
        assertThat(actual).isInstanceOf(NodeFolder::class.java)
        val actualAsFolder = actual as NodeFolder
        assertThat(actualAsFolder.isInRubbishBin).isTrue()
        assertThat(actualAsFolder.isPendingShare).isTrue()
    }

    private fun getMockNode(
        name: String = expectedName,
        size: Long = expectedSize,
        label: Int = expectedLabel,
        id: Long = expectedId,
        parentId: Long = expectedParentId,
        base64Id: String = expectedBase64Id,
        modificationTime: Long = expectedModificationTime,
        isFile: Boolean,
    ): MegaNode {
        val node = mock<MegaNode> {
            on { this.name }.thenReturn(name)
            on { this.size }.thenReturn(size)
            on { this.label }.thenReturn(label)
            on { this.handle }.thenReturn(id)
            on { this.parentHandle }.thenReturn(parentId)
            on { this.base64Handle }.thenReturn(base64Id)
            on { this.modificationTime }.thenReturn(modificationTime)
            on { this.isFile }.thenReturn(isFile)
            on { this.isFolder }.thenReturn(!isFile)
        }
        return node
    }
}