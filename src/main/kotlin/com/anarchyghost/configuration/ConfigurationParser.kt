package com.anarchyghost.configuration

import com.anarchyghost.models.common.DiscordEmbedType
import com.anarchyghost.models.common.EventType
import com.anarchyghost.models.json.configuration.ConfigurationJson
import com.anarchyghost.models.json.configuration.events.EventsConditionConfigurationJson
import com.anarchyghost.models.json.configuration.events.EventsConfigurationJson
import com.anarchyghost.models.json.configuration.events.EventsLabelsConditionConfigurationJson
import com.anarchyghost.models.json.configuration.events.senders.EventsSendersConfiguration
import com.anarchyghost.models.json.configuration.events.senders.EventsSendersMessageConfiguration
import com.anarchyghost.models.json.configuration.senders.SendersConfigurationJson
import com.anarchyghost.processing.EventProcessor
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator
import com.anarchyghost.processing.condition.evaluator.implementation.*
import com.anarchyghost.processing.event.listener.implementation.DefaultEventListener
import com.anarchyghost.processing.event.listener.implementation.ListenerType
import com.anarchyghost.processing.event.listener.implementation.SenderAndMessageGeneratorDto
import com.anarchyghost.processing.event.preprocessor.implementation.DefaultPreprocessor
import com.anarchyghost.processing.message.generator.MessageGenerator
import com.anarchyghost.processing.message.generator.implementation.DiscordMessageGenerator
import com.anarchyghost.processing.message.generator.implementation.EmbedGenerationRule
import com.anarchyghost.processing.message.generator.implementation.FieldsMessageGenerator
import com.anarchyghost.processing.message.generator.implementation.TextMessageGenerator
import com.anarchyghost.processing.message.sender.MessageSender
import com.anarchyghost.processing.message.sender.implementation.DiscordMessageSender
import com.anarchyghost.utils.JarClassLoader
import com.anarchyghost.utils.toByEventEvaluator
import com.anarchyghost.utils.toByEventStringEvaluator
import io.ktor.client.*

//TODO better messages
class ConfigurationParser(
    private val httpClient: HttpClient,
) {
    companion object {
        private val GROUP_EVENTS = setOf(EventType.MEMBER_EVENT, EventType.SUBGROUP_EVENT, EventType.OTHER)
        private val PROJECT_EVENTS = EventType.entries.toSet() - GROUP_EVENTS + EventType.OTHER
    }

    fun parse(configuration: ConfigurationJson): EventProcessor {
        val jarsSet = configuration.plugins.toSet()
        require(jarsSet.size == configuration.plugins.size) { "Jar path must be unique" }
        val classLoader = JarClassLoader(jarsSet)
        val usersMapping = configuration.users
        val labelsMapping = configuration.labels
        val senders = configuration.senders.associateBy { it.id }.mapValues { (_, sender) ->
            sender.parse(classLoader)
        }
        require(senders.keys.size == configuration.senders.size) { "Duplicated senders id not allowed" }
        val eventListeners =
            configuration.events.map { event -> event.parse(senders = senders, classLoader = classLoader) }
        return EventProcessor(
            eventListeners = eventListeners,
            labelsMapping = labelsMapping,
            usersMapping = usersMapping,
            preProcessor = DefaultPreprocessor(),
        )
    }

    private fun SendersConfigurationJson.parse(classLoader: JarClassLoader): MessageSender {
        require((this.discord != null) xor (this.custom != null)) { "Only one of [discord, custom] allowed in [sender]" }
        return when {
            this.discord != null -> {
                DiscordMessageSender(
                    url = this.discord!!.link,
                    httpClient = httpClient,
                )
            }

            this.custom != null -> {
                val clazz = classLoader.loadClass(this.custom!!.clazz) as? MessageSender
                checkNotNull(clazz) { "Error when loading class ${this.custom!!.clazz}" }
            }

            else -> {
                error("Unsupported sender type")
            }
        }

    }

    private fun EventsConfigurationJson.parse(
        senders: Map<String, MessageSender>, classLoader: JarClassLoader
    ): DefaultEventListener {
        require((this.projectIds == null) or (this.groupIds == null)) { "Only one of [projectIds, groupIds] allowed in [sender]" }
        val listenerType: ListenerType
        val ids: Set<String>
        when {
            this.projectIds != null -> {
                val projectIdsSet = this.projectIds!!.toSet()
                require(this.projectIds!!.size == projectIdsSet.size) { "Duplicated project id not allowed" }
                require(this.type in PROJECT_EVENTS) { "Event type [${this.type}] not support projectIds" }
                listenerType = ListenerType.PROJECT
                ids = projectIdsSet
            }

            this.groupIds != null -> {
                val groupIdsSet = this.groupIds!!.toSet()
                require(this.groupIds!!.size == groupIdsSet.size) { "Duplicated group id not allowed" }
                require(this.type in GROUP_EVENTS) { "Event type [${this.type}] not support groupIds" }
                listenerType = ListenerType.GROUP
                ids = groupIdsSet
            }

            else -> {
                listenerType = ListenerType.OTHERS
                ids = setOf()
            }
        }

        return DefaultEventListener(
            eventType = this.type,
            senders = this.senders.parse(
                senders = senders, classLoader = classLoader
            ),
            condition = this.condition.parse(classLoader),
            ids = ids,
            type = listenerType,
        )
    }

    private fun List<EventsSendersConfiguration>.parse(
        senders: Map<String, MessageSender>, classLoader: JarClassLoader
    ): List<SenderAndMessageGeneratorDto> {
        return this.map { senderConfiguration ->
            val sender = senders[senderConfiguration.id]
            requireNotNull(sender) { "Not found sender with id [${senderConfiguration.id}]" }
            SenderAndMessageGeneratorDto(
                sender = sender, generator = senderConfiguration.message.parse(classLoader),
            )
        }
    }

    private fun EventsSendersMessageConfiguration.parse(classLoader: JarClassLoader): MessageGenerator {
        require((this.discord != null) xor (this.text != null) xor (this.custom != null) xor (this.fields != null)) { "Only one of [discord, custom, text, fields] allowed in [message]" }
        return when {
            this.discord != null -> {
                DiscordMessageGenerator(
                    contentGenerationRule = this.discord!!.content.toByEventStringEvaluator(),
                    embedsGenerationRule = this.discord!!.embeds.map { embed ->
                        EmbedGenerationRule(type = { event ->
                            DiscordEmbedType.valueOf(
                                embed.type.toByEventStringEvaluator<String>()(
                                    event
                                )
                            )
                        },
                            title = embed.title?.toByEventStringEvaluator() ?: { null },
                            url = embed.url?.toByEventStringEvaluator() ?: { null },
                            description = embed.description?.toByEventStringEvaluator() ?: { null })
                    },
                )
            }

            this.fields != null -> {
                FieldsMessageGenerator(
                    generationRules = this.fields!!.mapValues { it.value.toByEventStringEvaluator() }
                )
            }

            this.text != null -> {
                TextMessageGenerator(
                    contentGenerationRule = this.text!!.content.toByEventStringEvaluator(),
                )
            }

            this.custom != null -> {
                val clazz = classLoader.loadClass(this.custom!!.clazz) as? MessageGenerator
                checkNotNull(clazz) { "Error when loading class ${this.custom!!.clazz}" }
            }

            else -> {
                error("Unsupported generator type")
            }
        }
    }

    private fun EventsConditionConfigurationJson.parse(classLoader: JarClassLoader): ConditionEvaluator {
        require((this.labels != null) xor (this.custom != null) xor (this.text != null) xor (this.not != null) xor (this.or != null) xor (this.and != null)) { "Only one of [labels, custom, text, not, and, or] allowed in [condition]" }
        return when {
            this.labels != null -> this.labels!!.parse()
            this.text != null -> TextConditionEvaluator(condition = text!!)
            this.not != null -> NotConditionEvaluator(condition = this.not!!.parse(classLoader))
            this.or != null -> {
                require(this.or!!.size > 1) { "Need at least 2 conditions for [or] evaluator" }
                OrConditionEvaluator(conditions = this.or!!.map { or -> or.parse(classLoader) })
            }

            this.and != null -> {
                require(this.and!!.size > 1) { "Need at least 2 conditions for [and] evaluator" }
                OrConditionEvaluator(conditions = this.and!!.map { and -> and.parse(classLoader) })
            }

            this.custom != null -> {
                val clazz = classLoader.loadClass(this.custom!!.clazz) as? ConditionEvaluator
                checkNotNull(clazz) { "Error when loading class ${this.custom!!.clazz}" }
            }

            else -> error("Unsupported condition type")
        }
    }

    private fun EventsLabelsConditionConfigurationJson.parse(): LabelsConditionEvaluator {
        require((this.exists != null) xor (this.deleted != null) xor (this.added != null)) { "Only one of [exists, deleted, added] allowed in [labels]" }
        return when {
            this.added != null -> LabelsConditionEvaluator(label = this.added!!, action = LabelsAction.ADDED)
            this.deleted != null -> LabelsConditionEvaluator(label = this.deleted!!, action = LabelsAction.DELETED)
            this.exists != null -> LabelsConditionEvaluator(label = this.exists!!, action = LabelsAction.EXISTS)
            else -> error("Unsupported label condition type")
        }
    }
}