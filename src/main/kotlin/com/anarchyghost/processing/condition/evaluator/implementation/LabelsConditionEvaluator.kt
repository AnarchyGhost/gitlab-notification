package com.anarchyghost.processing.condition.evaluator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator

enum class LabelsAction {
    ADDED,
    DELETED,
    EXISTS,
}

class LabelsConditionEvaluator(private val label: String, private val action: LabelsAction): ConditionEvaluator {
    override suspend fun evaluate(event: GitlabEvent<*>): Boolean {
        val set = when(action) {
            LabelsAction.ADDED -> event.addedLabels
            LabelsAction.DELETED -> event.deletedLabels
            LabelsAction.EXISTS -> event.labels
        }
        return label in set
    }

}