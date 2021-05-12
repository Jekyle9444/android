package mega.privacy.android.app.meeting.listeners

import android.graphics.Bitmap
import android.view.TextureView
import mega.privacy.android.app.meeting.MegaSurfaceRendererGroup
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

class GroupVideoListener(
    textureView: TextureView,
    peerId: Long,
    clientId: Long,
    isMe: Boolean,
) : MegaChatVideoListenerInterface {

    var width = 0
    var height = 0
    private var bitmap: Bitmap? = null
    var surfaceTexture: TextureView? = null
    private var isLocal = false
    var localRenderer: MegaSurfaceRendererGroup? = null

    override fun onChatVideoData(
        api: MegaChatApiJava,
        chatid: Long,
        width: Int,
        height: Int,
        byteBuffer: ByteArray
    ) {

        if (width == 0 || height == 0 || byteBuffer == null) {
            return
        }

        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            val viewWidth = surfaceTexture!!.width
            val viewHeight = surfaceTexture!!.height
            if (viewWidth != 0 && viewHeight != 0) {
                bitmap = localRenderer!!.createBitmap(width, height)
            } else {
                this.width = -1
                this.height = -1
            }
        }

        if (bitmap == null) return
        bitmap!!.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer))


        if (!isLocal || VideoCaptureUtils.isVideoAllowed()) {
            localRenderer!!.drawBitmap(isLocal)
        }
    }

    init {
        this.width = 0
        this.height = 0
        this.surfaceTexture = textureView
        this.isLocal = isMe
        this.localRenderer = MegaSurfaceRendererGroup(surfaceTexture, peerId, clientId)
    }

    fun getLastFrame(width: Int, height: Int): Bitmap? {
        return if (surfaceTexture != null) {
            surfaceTexture!!.getBitmap(width, height)
        } else null
    }
}