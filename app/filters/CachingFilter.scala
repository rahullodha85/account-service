
package filters

import javax.inject._

import akka.stream.Materializer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent._

class CachingFilter @Inject() (val mat: Materializer) extends Filter {
  def apply(next: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    next(req).map { result => result.withHeaders("pragma" -> "no-cache", "cache-control" -> "max-age=0, no-cache", "expires" -> "-1")
    }
  }
}