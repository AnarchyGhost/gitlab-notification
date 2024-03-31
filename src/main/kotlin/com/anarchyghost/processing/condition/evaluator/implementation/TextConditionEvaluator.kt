package com.anarchyghost.processing.condition.evaluator.implementation

import com.anarchyghost.models.domain.GitlabEvent
import com.anarchyghost.processing.condition.evaluator.ConditionEvaluator
import com.anarchyghost.utils.toByEventEvaluator

class TextConditionEvaluator(private val condition: String): ConditionEvaluator {
    override suspend fun evaluate(event: GitlabEvent<*>): Boolean = condition.toByEventEvaluator<Boolean>().invoke(event)
}