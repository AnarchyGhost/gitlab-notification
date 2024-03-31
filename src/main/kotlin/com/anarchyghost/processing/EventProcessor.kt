package com.anarchyghost.processing

import com.anarchyghost.models.domain.event.types.BaseEvent
import com.anarchyghost.models.json.configuration.labels.LabelsConfigurationJson
import com.anarchyghost.models.json.configuration.users.UsersConfigurationJson
import com.anarchyghost.processing.event.listener.EventListener
import com.anarchyghost.processing.event.preprocessor.EventPreprocessor
import io.ktor.util.logging.*

class EventProcessor(
    private val eventListeners: List<EventListener>,
    //TODO new dto
    private val labelsMapping: List<LabelsConfigurationJson>,
    //TODO new dto
    private val usersMapping: List<UsersConfigurationJson>,
    private val preProcessor: EventPreprocessor,
) {
    companion object {
        private val logger = KtorSimpleLogger("EventProcessor")
    }

    suspend fun process(
        event: BaseEvent,
    ) {
        val preProcessedEvent = preProcessor.preprocess(
            event = event, labelsMapping = labelsMapping, usersMapping = usersMapping,
        )
        eventListeners.forEach { eventListener ->
            try {
                eventListener.processEvent(preProcessedEvent)
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }
}