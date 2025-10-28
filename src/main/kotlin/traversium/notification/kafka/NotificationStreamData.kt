package traversium.notification.kafka

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class NotificationStreamData @JsonCreator constructor(
    @JsonProperty("timestamp") val timestamp: OffsetDateTime? = OffsetDateTime.now(),
    @JsonProperty("senderId") val senderId: String,
    @JsonProperty("receiverIds") val receiverIds: List<String>,
    @JsonProperty("collectionReferenceId") val collectionReferenceId: Long?,
    @JsonProperty("nodeReferenceId") val nodeReferenceId: Long?,
    @JsonProperty("commentReferenceId") val commentReferenceId: Long?,
    @JsonProperty("action") val action: String,
)