package com.anarchyghost.processing.event.preprocessor.implementation

import com.anarchyghost.models.common.EventType
import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.models.domain.event.Label
import com.anarchyghost.models.domain.event.types.*
import com.anarchyghost.models.json.configuration.labels.LabelsConfigurationJson
import com.anarchyghost.models.json.configuration.users.UsersConfigurationJson
import com.anarchyghost.processing.event.preprocessor.EventPreprocessor

class DefaultPreprocessor : EventPreprocessor {
    private fun List<Label>?.getSetOfTitles(): Set<String> = this?.mapNotNull { it.title }?.toSet() ?: setOf()
    //TODO maybe better way is parse to class ???
    override suspend fun preprocess(
        event: BaseEvent,
        labelsMapping: List<LabelsConfigurationJson>,
        usersMapping: List<UsersConfigurationJson>,
    ): GitlabEvent<*> {
        var currentLabels: Set<String> = setOf()
        var previousLabels: Set<String> = setOf()
        var labels: Set<String> = setOf()
        var projectId: Long? = null
        var groupId: Long? = null
        val type: EventType
        when(event) {
            is PushEvent -> {
                projectId = event.projectId
                type = EventType.PUSH_EVENT
            }
            is IssueEvent -> {
                projectId = event.project?.id
                labels = event.labels.getSetOfTitles()
                currentLabels = event.changes?.labels?.current.getSetOfTitles()
                previousLabels = event.changes?.labels?.previous.getSetOfTitles()
                type = EventType.ISSUE_EVENT
            }
            is TagEvent -> {
                projectId = event.projectId
                type = EventType.TAG_EVENT
            }

            is CommentEvent -> {
                projectId = event.projectId
                type = EventType.COMMENT_EVENT
            }

            is JobEvent -> {
                projectId = event.projectId
                type = EventType.JOB_EVENT
            }

            is MergeRequestEvent -> {
                projectId = event.project?.id
                labels = event.labels.getSetOfTitles()
                currentLabels = event.changes?.labels?.current.getSetOfTitles()
                previousLabels = event.changes?.labels?.previous.getSetOfTitles()
                type = EventType.MERGE_REQUEST_EVENT
            }

            is PipelineEvent -> {
                projectId = event.project?.id
                type = EventType.PIPELINE_EVENT
            }

            is WikiPageEvent -> {
                projectId = event.project?.id
                type = EventType.WIKI_PAGE_EVENT
            }
        }
        return GitlabEvent(
            projectId = projectId?.toString(),
            groupId = groupId?.toString(),
            addedLabels = currentLabels-previousLabels,
            deletedLabels = previousLabels-currentLabels,
            labels = labels,
            usersMapping = usersMapping,
            labelsMapping = labelsMapping,
            event = event,
            type = type,
        )
    }
}