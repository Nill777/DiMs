package com.distributed_messenger.data.network.webRTC

import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.SignalMessage
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import java.nio.ByteBuffer

class PeerConnectionManager(
    private val webRTCManager: WebRTCManager,
    private val peerId: PeerId,
    private val isInitiator: Boolean
) {
    private val tag = "PeerConnectionManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _incomingData = MutableSharedFlow<String>()
    val incomingData: SharedFlow<String> = _incomingData.asSharedFlow()

    private val _outgoingSignal = MutableSharedFlow<SignalMessage>()
    val outgoingSignal: SharedFlow<SignalMessage> = _outgoingSignal.asSharedFlow()

    init {
        Logger.log(tag, "Initializing for peer '$peerId'. IsInitiator: $isInitiator")
        setupPeerConnection()
    }

    private fun setupPeerConnection() {
        Logger.log(tag, "setupPeerConnection Setting up PeerConnection for '$peerId'")
        peerConnection = webRTCManager.createPeerConnection(object : PeerConnectionObserverAdapter() {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Logger.log(tag, "onIceCandidate Generated ICE candidate for '$peerId'")
                    val signal = SignalMessage.IceCandidate(
                        sdp = it.sdp,
                        sdpMid = it.sdpMid,
                        sdpMLineIndex = it.sdpMLineIndex
                    )
                    scope.launch { _outgoingSignal.emit(signal) }
                }
            }

            override fun onDataChannel(channel: DataChannel?) {
                Logger.log(tag, "onDataChannel Remote data channel received for '$peerId'")
                dataChannel = channel
                setupDataChannelObserver()
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Logger.log(tag, "onIceConnectionChange ICE connection state for '$peerId' changed to: $newState")
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                Logger.log(tag, "onSignalingChange Signaling state for '$peerId' changed to: $newState", LogLevel.DEBUG)
            }
        })

        if (isInitiator) {
            Logger.log(tag, "Creating data channel as initiator for '$peerId'")
            dataChannel = peerConnection?.createDataChannel("data_channel", DataChannel.Init())
            setupDataChannelObserver()
//            createOffer()
        }
    }

    fun handleIncomingSignal(signal: SignalMessage) {
        val signalType = signal::class.simpleName
        Logger.log(tag, "handleIncomingSignal Handling incoming '$signalType' from '$peerId'")
        when (signal) {
            is SignalMessage.Offer -> handleOffer(signal)
            is SignalMessage.Answer -> handleAnswer(signal)
            is SignalMessage.IceCandidate -> handleIceCandidate(signal)
        }
    }

    fun sendMessage(data: String) {
        val state = dataChannel?.state()
        if (state == DataChannel.State.OPEN) {
            Logger.log(tag, "sendMessage Sending data to '$peerId'. Size: ${data.length} bytes", LogLevel.DEBUG)
            val buffer = ByteBuffer.wrap(data.toByteArray())
            dataChannel?.send(DataChannel.Buffer(buffer, false))
        } else {
            Logger.log(tag, "sendMessage Could not send message to '$peerId'. DataChannel state is $state", LogLevel.WARN)
        }
    }

    suspend fun createOffer(): SignalMessage.Offer {
        val offerDeferred = CompletableDeferred<SignalMessage.Offer>()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let {
                            val offer = SignalMessage.Offer(sdp = it.description)
                            offerDeferred.complete(offer)
                        } ?: offerDeferred.completeExceptionally(Exception("SessionDescription is null"))
                    }
                    override fun onSetFailure(error: String?) {
                        offerDeferred.completeExceptionally(Exception("SetLocalDescription failed: $error"))
                    }
                }, desc)
            }
            override fun onCreateFailure(error: String?) {
                offerDeferred.completeExceptionally(Exception("CreateOffer failed: $error"))
            }
        }, MediaConstraints())

        return offerDeferred.await()
    }

    private fun handleOffer(offer: SignalMessage.Offer) {
        Logger.log(tag, "handleOffer")
        val sdp = SessionDescription(SessionDescription.Type.OFFER, offer.sdp)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                Logger.log(tag, "onSetSuccess Remote description (Offer) set successfully for '$peerId'")
                createAnswer()
            }
            override fun onSetFailure(error: String?) {
                Logger.log(tag, "onSetFailure Failed to set Remote Description (Offer) for '$peerId': $error", LogLevel.ERROR)
            }
        }, sdp)
    }

    private fun createAnswer() {
        Logger.log(tag, "createAnswer Creating Answer for '$peerId'")
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Logger.log(tag, "onCreateSuccess Answer created successfully for '$peerId'")
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let {
                            Logger.log(tag, "onSetSuccess Local description (Answer) set successfully for '$peerId'")
                            val answer = SignalMessage.Answer(
                                sdp = it.description
                            )
                            scope.launch { _outgoingSignal.emit(answer) }
                        }
                    }
                    override fun onSetFailure(error: String?) {
                        Logger.log(tag, "onSetFailure Failed to set Local Description (Answer) for '$peerId': $error", LogLevel.ERROR)
                    }
                }, desc)
            }
            override fun onCreateFailure(error: String?) {
                Logger.log(tag, "onCreateFailure Failed to create Answer for '$peerId': $error", LogLevel.ERROR)
            }
        }, MediaConstraints())
    }

    private fun handleAnswer(answer: SignalMessage.Answer) {
        Logger.log(tag, "handleAnswer")
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, answer.sdp)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                Logger.log(tag, "onSetSuccess Remote description (Answer) set successfully for '$peerId'")
            }
            override fun onSetFailure(error: String?) {
                Logger.log(tag, "onSetFailure Failed to set Remote Description (Answer) for '$peerId': $error", LogLevel.ERROR)
            }
        }, sdp)
    }

    private fun handleIceCandidate(candidate: SignalMessage.IceCandidate) {
        val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        Logger.log(tag, "handleIceCandidate Adding received ICE candidate for '$peerId'", LogLevel.DEBUG)
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun setupDataChannelObserver() {
        Logger.log(tag, "setupDataChannelObserver Registering observer for DataChannel with '$peerId'")
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    val bytes = ByteArray(it.data.remaining())
                    it.data.get(bytes)
                    Logger.log(tag, "onMessage Received message on DataChannel from '$peerId'. Size: ${bytes.size} bytes")
                    scope.launch { _incomingData.emit(String(bytes)) }
                }
            }
            override fun onStateChange() {
                Logger.log(tag, "onStateChange DataChannel state for '$peerId' changed to: ${dataChannel?.state()}")
            }
            override fun onBufferedAmountChange(previousAmount: Long) {}
        })
    }

    fun close() {
        Logger.log(tag, "close Closing connection for '$peerId'")
        dataChannel?.close()
        peerConnection?.close()
        scope.cancel()
    }
}

// Вспомогательные классы остаются без изменений
open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

open class PeerConnectionObserverAdapter : PeerConnection.Observer {
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidate(candidate: IceCandidate?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onAddStream(stream: MediaStream?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onDataChannel(channel: DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
}