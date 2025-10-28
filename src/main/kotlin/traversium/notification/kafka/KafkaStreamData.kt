package traversium.notification.kafka

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class KafkaStreamData @JsonCreator constructor(
    @JsonProperty("timestamp") val timestamp: OffsetDateTime,
    @JsonProperty("senderId") val senderId: String,
    @JsonProperty("receiverIds") val receiverIds: List<String>,
    @JsonProperty("message") val message: String,
    @JsonProperty("referenceId") val referenceId: Long
)