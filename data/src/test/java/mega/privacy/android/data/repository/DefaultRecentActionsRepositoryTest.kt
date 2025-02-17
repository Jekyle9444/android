package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultRecentActionsRepositoryTest {
    private lateinit var underTest: RecentActionsRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val recentActionsMapper = mock<RecentActionsMapper>()

    private val recentActionBucketMapper = mock<RecentActionBucketMapper>()

    @Before
    fun setUp() {
        underTest = DefaultRecentActionsRepository(
            megaApiGateway = megaApiGateway,
            recentActionsMapper = recentActionsMapper,
            recentActionBucketMapper = recentActionBucketMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that get recent actions returns the result of api recentActions`() = runTest {
        val megaApiJava = mock<MegaApiJava>()
        val bucketList = mock<MegaRecentActionBucketList> { on { size() }.thenReturn(4) }
        val request = mock<MegaRequest> { on { recentActions }.thenReturn(bucketList) }
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val list = (1..4).map { mock<MegaRecentActionBucket>() }
        val recentActionBucket = RecentActionBucketUnTyped(
            isMedia = true,
            isUpdate = true,
            timestamp = 0L,
            parentNodeId = NodeId(1L),
            userEmail = "1",
            nodes = emptyList(),
        )
        val expected = (1..4).map { recentActionBucket }

        whenever(megaApiGateway.getRecentActionsAsync(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                error
            )
        }
        whenever(megaApiGateway.copyBucket(any())).thenReturn(mock())
        whenever(recentActionsMapper(any(), any())).thenReturn(list)
        whenever(
            recentActionBucketMapper.invoke(
                mock(),
            )
        ).thenReturn(recentActionBucket)

        assertThat(underTest.getRecentActions().size).isEqualTo(expected.size)
    }
}
