package com.anarchyghost.utils

import com.anarchyghost.models.domain.GitlabEvent
import javax.script.ScriptEngineManager

//TODO check evaluation safety
fun evaluate(expression: String, params: Map<String,Any>): Any? {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        params.forEach {param ->
            engine.put(param.key, param.value)
        }
        return engine.eval(expression)
    }

fun <T> String.toByEventEvaluator(): (GitlabEvent<*>) -> T = { event -> evaluate(this, mapOf("data" to event)) as T  }

fun <T> String.toByEventStringEvaluator(): (GitlabEvent<*>) -> T = { event -> evaluate("\"${this}\"", mapOf("data" to event)) as T  }