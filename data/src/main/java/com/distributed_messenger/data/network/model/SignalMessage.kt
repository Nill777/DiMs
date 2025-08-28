package com.distributed_messenger.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Сообщения для WebRTC сигнализации.
 * Sealed-интерфейс, а не sealed-класс, чтобы не иметь своих полей.
 */
sealed interface SignalMessage {
    val type: String

    data class Offer(
        @SerializedName("type") override val type: String = "OFFER",
        @SerializedName("sdp") val sdp: String
    ) : SignalMessage

    data class Answer(
        @SerializedName("type") override val type: String = "ANSWER",
        @SerializedName("sdp") val sdp: String
    ) : SignalMessage

    data class IceCandidate(
        @SerializedName("type") override val type: String = "ICE_CANDIDATE",
        @SerializedName("sdp") val sdp: String,
        @SerializedName("sdpMid") val sdpMid: String,
        @SerializedName("sdpMLineIndex") val sdpMLineIndex: Int
    ) : SignalMessage

    data class IceCandidates(
        @SerializedName("type") override val type: String = "ICE_CANDIDATES",
        @SerializedName("candidates") val candidates: List<IceCandidate>
    ) : SignalMessage
}