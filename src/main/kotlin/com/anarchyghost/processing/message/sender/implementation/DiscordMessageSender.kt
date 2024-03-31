package com.anarchyghost.processing.message.sender.implementation

import com.anarchyghost.models.domain.message.Message
import com.anarchyghost.models.domain.message.discord.DiscordMessage
import com.anarchyghost.processing.message.sender.MessageSender
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class DiscordMessageSender(
    private val httpClient: HttpClient,
    private val url: String,
) : MessageSender {
    override suspend fun send(message: Message) {
        check(message is DiscordMessage) { "Unsupported message type for discord sender" }
        httpClient.request(url) {
            contentType(ContentType.Application.Json)
            method = HttpMethod.Post
            setBody(message)
        }
    }
}