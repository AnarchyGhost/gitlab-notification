package com.anarchyghost.processing.message.generator.implementation

import com.anarchyghost.models.common.DiscordEmbedType
import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.models.domain.message.discord.DiscordEmbed
import com.anarchyghost.models.domain.message.discord.DiscordMessage
import com.anarchyghost.processing.message.generator.MessageGenerator

data class EmbedGenerationRule(
    val type: (GitlabEvent<*>) -> DiscordEmbedType,
    val title: (GitlabEvent<*>) -> String?,
    val description: (GitlabEvent<*>) -> String?,
    val url: (GitlabEvent<*>) -> String?,
)


class DiscordMessageGenerator(
    private val contentGenerationRule: (GitlabEvent<*>) -> String,
    private val embedsGenerationRule: List<EmbedGenerationRule>
) : MessageGenerator {
    override suspend fun generate(event: GitlabEvent<*>): DiscordMessage =
        DiscordMessage(
            content = contentGenerationRule(event),
            embeds = embedsGenerationRule.map { embedRule ->
                DiscordEmbed(
                    type = embedRule.type(event),
                    title = embedRule.title(event),
                    description = embedRule.description(event),
                    url = embedRule.url(event),
                )
            }
        )
}