import akka.actor.{ActorSystem, Scheduler}
import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.Success


@Singleton
class Filters @Inject() (timeoutFilter: TimeoutFilter) extends DefaultHttpFilters(timeoutFilter)


@Singleton
class TimeoutFilter @Inject() (
    actorSystem: ActorSystem)(
    override implicit val mat: Materializer) extends Filter {

  private implicit def executionContext = actorSystem.dispatcher

  val Timeout = 1.seconds

  val ErrorResult: Result = Results.ServiceUnavailable(
    JsObject(Map("message" -> JsString("Service timed out serving request")))
  )

  val logger = Logger(getClass)

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val resultPromise: Promise[Result] = Promise[Result]()

    // Schedule a timeout callback  in case the result takes too long to execute.
    actorSystem.scheduler.scheduleOnce(Timeout) {
      val timedOut: Boolean = resultPromise.tryComplete(Success(ErrorResult))
      if (timedOut) { logger.debug("Request timed out") }
    }

    // Begin processing the result. We process it on another thread so we can return
    // the promised result immediately.
    executionContext.execute(() => {
      val nextResult: Future[Result] = next(rh)
      resultPromise.tryCompleteWith(nextResult)
    })

    resultPromise.future
  }
}