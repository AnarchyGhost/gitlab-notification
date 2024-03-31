package com.anarchyghost.configuration

import com.anarchyghost.models.json.configuration.ConfigurationJson
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class ConfigurationReader {
    fun read(): ConfigurationJson {
        val path = System.getenv("CONFIG_PATH") ?: "config.json"
        val file = Paths.get(path)
        check(file.isRegularFile()) {"Configuration file not found"}
        return Json.decodeFromString(file.readText())
    }
}