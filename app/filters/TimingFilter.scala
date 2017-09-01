
package filters

import javax.inject.Inject

import akka.stream.Materializer
import monitoring.StatsDClientLike
import play.api.mvc._

import scala.concurrent._

class TimingFilter @Inject() (recorder: StatsDClientLike, val mat: Materializer) extends Filter {
  def apply(next: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    recorder.time("", req) {
      next(req)
    }
  }
}

