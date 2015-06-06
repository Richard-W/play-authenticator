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
import play.api.inject._
import javax.inject._

class AuthenticatorModule extends Module {

  def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = Seq(
    bind[Authenticator].to[AuthenticatorImpl].in[Singleton],
    bind[PrincipalController].to[PrincipalControllerImpl].in[Singleton]
  )
}
