package com.distributed_messenger.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Сообщения для WebRTC сигнализации.
 */
sealed class SignalMessage {
    data class Offer(
        @SerializedName("sdp") val sdp: String
    ) : SignalMessage()

    data class Answer(
        @SerializedName("sdp") val sdp: String
    ) : SignalMessage()

    data class IceCandidate(
        @SerializedName("sdp") val sdp: String,
        @SerializedName("sdpMid") val sdpMid: String,
        @SerializedName("sdpMLineIndex") val sdpMLineIndex: Int
    ) : SignalMessage()
}