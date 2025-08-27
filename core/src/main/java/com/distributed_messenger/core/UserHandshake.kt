package com.distributed_messenger.core

import java.util.UUID

// Data-класс для обмена по P2P каналу
data class UserHandshake(
    val userId: UUID,
    val username: String
)