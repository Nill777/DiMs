package com.distributed_messenger.data.network.webRTC

import android.content.Context
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory

class WebRTCManager(context: Context) {
    private val tag = "WebRTCManager"
    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory

    val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    init {
        Logger.log(tag, "Initializing PeerConnectionFactory...")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        Logger.log(tag, "PeerConnectionFactory created successfully.")
    }

    fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        Logger.log(tag, "Creating a new PeerConnection object.", LogLevel.DEBUG)
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)
    }

    fun release() {
        Logger.log(tag, "Releasing WebRTC resources.")
        peerConnectionFactory.dispose()
        eglBase.release()
    }
}