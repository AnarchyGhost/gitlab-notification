package com.anarchyghost.plugins

import com.anarchyghost.configuration.ConfigurationParser
import com.anarchyghost.configuration.ConfigurationReader
import com.anarchyghost.models.domain.event.types.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                allowStructuredMapKeys = true
                explicitNulls = false
                coerceInputValues = true
                ignoreUnknownKeys = true
            })
        }
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    val eventProcessor = ConfigurationParser(httpClient).parse(ConfigurationReader().read())
    routing {
        post("/") {
//            println(call.receiveText())
            val event: BaseEvent = when (call.request.headers["X-Gitlab-Event"]) {
                "Push Hook" -> call.receive<PushEvent>()
                "Tag Push Hook" -> call.receive<TagEvent>()
                "Issue Hook" -> call.receive<IssueEvent>()
                "Note Hook" -> call.receive<CommentEvent>()
                "Merge Request Hook" -> call.receive<MergeRequestEvent>()
                "Wiki Page Hook" -> call.receive<WikiPageEvent>()
                "Pipeline Hook" -> call.receive<PipelineEvent>()
                "Job Hook" -> call.receive<JobEvent>()

//                "Deployment Hook" -> call.receive<IssueEvent>()
//                "Feature Flag Hook" -> call.receive<IssueEvent>()
//                "Release Hook" -> call.receive<IssueEvent>()
//                "Emoji Hook" -> call.receive<IssueEvent>()
//                "Resource Access Token Hook" -> call.receive<IssueEvent>()
//                "Member Hook" -> call.receive<IssueEvent>()
//                "Subgroup Hook" -> call.receive<IssueEvent>()
//                "System Hook" -> call.receive<IssueEvent>()
//                else -> call.receive<IssueEvent>()
                else -> error("Unknown event")
            }
            eventProcessor.process(event = event)
            call.respond(HttpStatusCode.OK)
        }
    }
}
