package com.anarchyghost.processing.condition.evaluator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator

class OrConditionEvaluator(
    private val conditions: List<ConditionEvaluator>,
) : ConditionEvaluator {
    override suspend fun evaluate(event: GitlabEvent<*>): Boolean =
        conditions.any { condition -> condition.evaluate(event) }
}