package controllers

import java.util.Random

import javax.inject._
import play.api.mvc._

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private val random = new Random

  def index() = Action { implicit request: Request[AnyContent] =>
    val sleepTime = random.nextInt(2000) // Sleep for 0 - 2 seconds
    Thread.sleep(sleepTime)
    Ok(s"Slept for ${sleepTime}ms")
  }
}
