package com.anarchyghost.utils

import com.anarchyghost.models.domain.GitlabEvent
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

//TODO check evaluation safety
fun evaluate(expression: String, params: Map<String, Any>): Any? {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
    params.forEach { param ->
        engine.put(param.key, param.value)
    }
    return engine.eval(expression)
}

private fun process(text: String): String {
    val pattern = Regex("#\\{([^{}]+)}")
    return pattern.replace(text) {
        "dataEvaluator.evaluate(data, \"${it.groupValues[1]}\")"
    }
}

object DataAccessEvaluator {
    fun evaluate(event: GitlabEvent<*>, value: String): Any {
        val splitted = value.split(".")
        var current: Any = event
        splitted.forEach { currentPart ->
            val currentPartValue = if(currentPart.startsWith('[') && currentPart.endsWith(']')) evaluate(event, currentPart.removePrefix("[").removeSuffix("]")).toString() else currentPart
            when (current) {
                is List<*> -> {
                    current = (current as List<*>)[currentPartValue.toIntOrNull()!!]!!
                }
                is Map<*, *> -> {
                    current = (current as Map<*, *>)[currentPartValue]!!
                }
                else -> {
                    current = (current::class as KClass<in Any>).memberProperties.first { it.name == currentPartValue }
                        .invoke(current)!!
                }
            }
        }
        return current
    }
}

fun <T> String.toByEventEvaluator(): (GitlabEvent<*>) -> T = { event ->
    evaluate(
        process(this),
        mapOf("data" to event, "dataEvaluator" to DataAccessEvaluator)
    ) as T
}

fun <T> String.toByEventStringEvaluator(): (GitlabEvent<*>) -> T = { event ->
    evaluate(
        "\"${process(this)}\"",
        mapOf("data" to event, "dataEvaluator" to DataAccessEvaluator)
    ) as T
}