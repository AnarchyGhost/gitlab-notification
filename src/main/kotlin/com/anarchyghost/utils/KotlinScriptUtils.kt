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
    println("Evaluating $expression")
    return engine.eval(expression)
}

private fun process(text: String): String {
    val pattern = Regex("#\\{([^{}]+)}")
    return pattern.replace(text) {
        "dataEvaluator.getTextValue(data, \"${it.groupValues[1]}\")"
    }
}

object DataAccessEvaluator {
    fun evaluate(event: GitlabEvent<*>, value: String): Any {
        val splitted = value.split(".", "[", "].")
        var current: Any = event
        splitted.forEach { spCurrent ->
            when (current) {
                is List<*> -> {
                    current = (current as List<*>)[spCurrent.removeSuffix("]").toIntOrNull()!!]!!
                }
                is Map<*, *> -> {
                    current = (current as Map<*, *>)[spCurrent.removeSuffix("]")]!!
                }
                else -> {
                    current = (current::class as KClass<in Any>).memberProperties.first { it.name == spCurrent }
                        .invoke(current)!!
                }
            }
        }
        return current
    }
}

fun <T> String.toByEventEvaluator(): (GitlabEvent<*>) -> T = { event ->
    evaluate(
        this,
        mapOf("data" to event, "dataEvaluator" to DataAccessEvaluator)
    ) as T
}

fun <T> String.toByEventStringEvaluator(): (GitlabEvent<*>) -> T = { event ->
    evaluate(
        "\"${process(this)}\"",
        mapOf("data" to event, "dataEvaluator" to DataAccessEvaluator)
    ) as T
}