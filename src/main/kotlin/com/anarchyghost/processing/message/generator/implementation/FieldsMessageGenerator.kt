package com.anarchyghost.processing.message.generator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.models.domain.message.discord.DiscordEmbed
import com.anarchyghost.models.domain.message.discord.DiscordMessage
import com.anarchyghost.models.domain.message.fields.FieldsMessage
import com.anarchyghost.processing.message.generator.MessageGenerator

class FieldsMessageGenerator(
    private val generationRules: Map<String, (GitlabEvent<*>) -> String>
) : MessageGenerator {
    override suspend fun generate(event: GitlabEvent<*>): FieldsMessage =
        FieldsMessage(generationRules.mapValues { it.value.invoke(event) })
}