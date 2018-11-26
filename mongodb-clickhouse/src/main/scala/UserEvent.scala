
import java.util.Date

import org.bson.types.ObjectId

case class UserEvent(timestamp: Date,
                     userId: String,
                     eventType: String,
                    )

case class User(id : ObjectId,
                name: String,
                createdAt : Int,
                updatedAt: Int,
                )

case class Logs(id: ObjectId,
                userId: ObjectId,
                event: String,
                success: Boolean,
                dateTime: Int,
               )