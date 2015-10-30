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
import play.api.libs.openid.OpenID
import javax.inject._
import reactivemongo.api._
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.Future

trait AuthenticatorApi {

  /** Get the currently authed principal */
  def principal()(implicit request: Request[AnyContent]): Future[Option[Principal]]

  /** Authenticate a principal using its username and password
    *
    * Provides a controller action that wraps the whole login process. It needs
    * a credentials function that extracts the username and password from the request
    * and a policy function that decides whether or not to allow login for a
    * given [[Principal]]
    *
    * {{{
    * def login = authenticator.authenticateWithPassword({ request ⇒
    *   val body = request.body.asFormUrlEncoded.get
    *   (body("username")(0), body("password")(0))
    * })({
    *   case Some(princ) ⇒
    *     if(princ.value[Boolean]("activated").getOrElse(false)) {
    *       Future.successful((true, Ok("Logged in")))
    *     } else {
    *       Future.successful((false, Ok("Not yet activated")))
    *     }
    *   case None ⇒
    *     Future.successful((false, Ok("Wrong credentials")))
    * })
    * }}}
    */
  def authenticateWithPassword(credentials: Request[AnyContent] ⇒ (String, String))(policy: Option[Principal] ⇒ Future[(Boolean, Result)]): Action[AnyContent]

  /** Authenticate a principal using its openid
    *
    * The future this function return might fail if the openid authentication process fails at retrieving
    * the redirect URL. You should handle this failure properly.
    */
  def authenticateWithOpenID(openid: String, callback: Call, axRequired: Seq[(String, String)] = Seq.empty, axOptional: Seq[(String, String)] = Seq.empty, realm: Option[String] = None)(implicit request: Request[AnyContent]): Future[Result]

  /** Callback for openid
    *
    * If the openID is valid and a principal is found a [[Some[Principal]]] and [[Some[String]]] (the valid openID) is passed to res.
    * If the openID is valid but no principal can be found [[None]] and [[Some[String]]] is passed to res.
    * If the openID is invalid [[None]] and [[None]] is passed to res.
    * The third parameter contains a map of the exchange attributes.
    *
    * The boolean return value of res can be used to prevent a login even though the credentials are
    * correct.
    *
    * When the principal is not existing but the openID is valid res should trigger some kind of application specific registration
    * process.
    */
  def openIDCallback(res: (Option[Principal], Option[String], Map[String, String]) ⇒ Future[(Boolean, Result)])(implicit request: Request[AnyContent]): Future[Result]

  /** Unauthenticate a principal */
  def unauthenticate(result: Result)(implicit request: Request[AnyContent]): Future[Result]
}

private[authenticator] class AuthenticatorApiImpl @Inject()(
  val principals: PrincipalsApi,
  actorSystem: ActorSystem,
  application: Application
) extends AuthenticatorApi with Results {

  /* Get an execution context */
  import actorSystem.dispatcher

  def principal()(implicit request: Request[AnyContent]): Future[Option[Principal]] = {
    request.session.get("authenticatorID") match {
      case Some(id) ⇒ principals.findByID(id)
      case None ⇒ Future.successful(None)
    }
  }

  def authenticateWithPassword(credentials: Request[AnyContent] ⇒ (String, String))(policy: Option[Principal] ⇒ Future[(Boolean, Result)]): Action[AnyContent] = Action.async { request ⇒
    val (user, pass) = credentials(request)
    principals.findByName(user) flatMap {
      case Some(princ) ⇒
        if(princ.verifyPass(pass)) {
          policy(Some(princ)) map {
            case (true, result) ⇒ result.withSession(request.session + ("authenticatorID" -> princ.id))
            case (false, result) ⇒ result
          }
        } else {
          policy(None) map { case (_, result) ⇒ result }
        }
      case None ⇒
        policy(None) map { case (_, result) ⇒ result }
    }
  }

  def authenticateWithOpenID(openid: String, callback: Call, axRequired: Seq[(String, String)] = Seq.empty, axOptional: Seq[(String, String)] = Seq.empty, realm: Option[String] = None)(implicit request: Request[AnyContent]): Future[Result] = {
    OpenID.redirectURL(openid, callback.absoluteURL, axRequired, axOptional, realm)(application) map { Redirect(_) }
  }

  def openIDCallback(res: (Option[Principal], Option[String], Map[String, String]) ⇒ Future[(Boolean, Result)])(implicit request: Request[AnyContent]): Future[Result] = {
    (OpenID.verifiedId(request, application) flatMap { userInfo ⇒
      principals.findByOpenID(userInfo.id) flatMap { princOption ⇒
        princOption match {
          case Some(princ) ⇒
            res(Some(princ), Some(userInfo.id), userInfo.attributes) map {
              case (true, result) ⇒ result.withSession(request.session + ("authenticatorID" -> princ.id))
              case (false, result) ⇒ result
            }
          case None ⇒
            res(None, Some(userInfo.id), userInfo.attributes) map { case (_, result) ⇒ result }
        }
      }
    }).recoverWith {
      case t: Throwable ⇒ res(None, None, Map.empty) map { case (_, result) ⇒ result }
    }
  }

  def unauthenticate(result: Result)(implicit request: Request[AnyContent]): Future[Result] = {
    Future.successful(result.withSession(request.session - "authenticatorID"))
  }
}
