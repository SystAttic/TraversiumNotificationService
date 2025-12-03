package traversium.notification.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class NotificationStreamData(
    @JsonProperty("timestamp")
    val timestamp: OffsetDateTime? = OffsetDateTime.now(),
    @JsonProperty("senderId") val senderId: String,
    @JsonProperty("receiverIds") val receiverIds: List<String>,
    @JsonProperty("collectionReferenceId") val collectionReferenceId: Long?,
    @JsonProperty("nodeReferenceId") val nodeReferenceId: Long?,
    @JsonProperty("mediaReferenceId") val mediaReferenceId: Long?,
    @JsonProperty("commentReferenceId") val commentReferenceId: Long?,
    @JsonProperty("action") val action: ActionType,
)

enum class ActionType {
    CREATE,
    UPDATE,
    DELETE,
    ADD,
    REMOVE,
    REARRANGE,
    CHANGE_TITLE,
    CHANGE_DESCRIPTION,
    CHANGE_COVER_PHOTO,
    LIKE,
    REPLY,
    FOLLOW,
    ADD_COLLABORATOR,
    ADD_VIEWER,
    REMOVE_COLLABORATOR,
    REMOVE_VIEWER,
}