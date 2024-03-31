package com.anarchyghost.processing.message.generator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.models.domain.message.Message
import com.anarchyghost.models.domain.message.text.TextMessage
import com.anarchyghost.processing.message.generator.MessageGenerator

class TextMessageGenerator(
    private val contentGenerationRule: (GitlabEvent<*>) -> String,
) : MessageGenerator {
    override suspend fun generate(event: GitlabEvent<*>): Message =
        TextMessage(
            content = contentGenerationRule(event),
        )
}