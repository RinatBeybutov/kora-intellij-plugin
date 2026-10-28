package io.github.dsudomoin.koraplugin.endpoints

/** Where an endpoint declaration came from. */
enum class EndpointSource { ANNOTATION, OPENAPI }

/**
 * Navigation pointer deflated to strings: safe to keep across threads and PSI updates.
 * The element is re-resolved from these coordinates at navigation time.
 */
data class EndpointTarget(
    val classFqn: String?,
    val methodName: String?,
    val specFileUrl: String?,
    val offset: Int,
) {
    companion object {
        fun method(classFqn: String, methodName: String) = EndpointTarget(classFqn, methodName, null, 0)

        fun spec(specFileUrl: String, offset: Int) = EndpointTarget(null, null, specFileUrl, offset)
    }
}

data class KoraEndpoint(
    val verb: String,
    val path: String,
    val operationId: String?,
    val summary: String?,
    val sources: Set<EndpointSource>,
    val targets: List<EndpointTarget>,
)

/** Incoming Kafka topic: @KafkaListener with the topic in its `value` attribute. */
data class KafkaConsumer(val topic: String, val targets: List<EndpointTarget>)

/**
 * Outgoing Kafka topics: @KafkaPublisher interface with @KafkaPublisher.Topic methods.
 * [configPath] is the publisher's `value` attribute (producer config key), if present.
 */
data class KafkaPublisher(
    val publisherFqn: String,
    val configPath: String?,
    val topics: List<KafkaPublisherTopic>,
)

data class KafkaPublisherTopic(val topic: String, val target: EndpointTarget)

/** Database entity: @Table class; [tableName] from the annotation value. */
data class DbEntity(val tableName: String, val classFqn: String, val target: EndpointTarget)

data class KoraEndpointsSnapshot(
    val endpoints: List<KoraEndpoint>,
    val consumers: List<KafkaConsumer>,
    val publishers: List<KafkaPublisher>,
    val entities: List<DbEntity>,
) {
    val isEmpty: Boolean
        get() = endpoints.isEmpty() && consumers.isEmpty() && publishers.isEmpty() && entities.isEmpty()

    companion object {
        val EMPTY = KoraEndpointsSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
    }
}

/** Join a controller-level prefix with a route path: ("/api", "/pets/") → "/api/pets". */
fun normalizePath(prefix: String, path: String): String {
    val head = prefix.trim().trimEnd('/')
    val tail = path.trim().trimStart('/')
    return "/$head/$tail"
        .replace(Regex("/{2,}"), "/")
        .let { if (it.length > 1) it.trimEnd('/') else it }
}

fun normalizeVerb(verb: String): String = verb.trim().uppercase()
