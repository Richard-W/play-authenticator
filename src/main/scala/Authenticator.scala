/* Copyright 2015 Richard Wiedenhöft <richard@wiedenhoeft.xyz>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.modules.authenticator

import play.api._
import play.api.mvc._
import play.api.inject._
import javax.inject._
import reactivemongo.api._
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.Future
import play.modules.reactivemongo.ReactiveMongo

trait Authenticator {
  /** The principal controller. May be used to manage principals */
  def principals: PrincipalController

  /** Get the currently authed principal */
  def principal()(implicit request: Request[AnyContent]): Future[Option[Principal]]

  /** Authenticate a principal using its username and password */
  def authenticate(name: String, pass: String)(res: (Option[Principal]) ⇒ Future[(Boolean, Result)])(implicit request: Request[AnyContent]): Future[Result]
}

final class AuthenticatorImpl @Inject()(
  val principals: PrincipalController,
  actorSystem: ActorSystem
) extends Authenticator {

  /* Get an execution context */
  import actorSystem.dispatcher

  def principal()(implicit request: Request[AnyContent]): Future[Option[Principal]] = {
    request.session.get("authenticatorPrincipal") match {
      case Some(princName) ⇒ principals.find(princName)
      case None ⇒ Future.successful(None)
    }
  }

  def authenticate(name: String, pass: String)(res: (Option[Principal]) ⇒ Future[(Boolean, Result)])(implicit request: Request[AnyContent]): Future[Result] = {
    principals.find(name) flatMap {
      case Some(princ) ⇒
        if(princ.pass.verify(pass)) {
          res(Some(princ)) map {
            case (true, result) ⇒ result.withSession(request.session + ("authenticatorPrincipal" -> princ.name))
            case (false, result) ⇒ result
          }
        } else {
          res(None) map { case (_, result) ⇒ result }
        }
      case None ⇒
        res(None) map { case (_, result) ⇒ result }
    }
  }
}
