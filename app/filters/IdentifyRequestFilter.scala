
package filters

import java.util.UUID
import javax.inject._

import akka.stream.Materializer
import org.slf4j.MDC
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent._

class IdentifyRequestFilter @Inject() (val mat: Materializer) extends Filter {
  def apply(next: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    val correlationId: String = req.headers.get("correlation-id").getOrElse(UUID.randomUUID().toString)
    MDC.put("correlation-id", correlationId)
    val request = req.copy(headers = req.headers.add(("correlation-id", correlationId)))
    next(request).map(result => result.withHeaders("correlation-id" -> correlationId))
  }
}
