package com.anarchyghost.processing.event.listener.implementation

import com.anarchyghost.models.common.EventType
import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator
import com.anarchyghost.processing.event.listener.EventListener
import com.anarchyghost.processing.message.generator.MessageGenerator
import com.anarchyghost.processing.message.sender.MessageSender
import io.ktor.util.logging.*

class SenderAndMessageGeneratorDto(
    val sender: MessageSender,
    val generator: MessageGenerator,
)

enum class ListenerType {
    GROUP, PROJECT, OTHERS,
}

class DefaultEventListener(
    private val eventType: EventType,
    private val type: ListenerType,
    private val ids: Set<String>,
    private val condition: ConditionEvaluator,
    private val senders: List<SenderAndMessageGeneratorDto>
) : EventListener {
    companion object {
        val logger = KtorSimpleLogger("DefaultEventListener")
    }

    override suspend fun processEvent(event: GitlabEvent<*>) {
        if (event.type != this.eventType) return
        if (type == ListenerType.PROJECT && event.projectId !in ids) return
        if (type == ListenerType.GROUP && event.groupId !in ids) return
        val evaluationResult = condition.evaluate(event)
        println("Evaluation result $evaluationResult")
        if (!evaluationResult) return
        senders.forEach { senderAndGenerator ->
            try {
                val generated = senderAndGenerator.generator.generate(event)
                println("Send $generated")
                senderAndGenerator.sender.send(generated)
            } catch (e: Exception) {
                //TODO good message
                logger.error("Failed to send message")
                logger.error(e)
            }
        }
    }
}