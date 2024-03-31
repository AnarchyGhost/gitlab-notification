package com.anarchyghost.processing.condition.evaluator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator

class NotConditionEvaluator(
    private val condition: ConditionEvaluator,
): ConditionEvaluator {
    override suspend fun evaluate(event: GitlabEvent<*>): Boolean =
        !condition.evaluate(event)
}