
package filters

import javax.inject._

import akka.stream.Materializer
import helpers.ControllerPayload
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent._

class ExceptionFilter @Inject() (val mat: Materializer) extends Filter with ControllerPayload {
  def apply(next: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    next(req) recover defaultExceptionHandler(req)
  }
}

