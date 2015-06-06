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

import org.scalatest._
import com.github.simplyscala.{ MongoEmbedDatabase, MongodProps }
import play.api.inject.guice._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

class ReactiveMongoModuleSpec extends FlatSpec with Matchers with BeforeAndAfter with MongoEmbedDatabase {

  val mongoURI = "mongodb://localhost:12345/test"
  val application = new GuiceApplicationBuilder()
    .configure("mongo.uri" -> mongoURI)
    .bindings(new ReactiveMongoModule)
    .bindings(new AuthenticatorModule)
    .build
  val injector = application.injector

  var mongoProps: MongodProps = null

  before {
    mongoProps = mongoStart()
  }

  after {
    Await.result(application.stop, Duration.Inf)
    mongoStop(mongoProps)
  }

  "AuthenticatorModule" should "supply a working Authenticator" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    Await.result(authenticator.principals.create("testuser", "testpass"), Duration.Inf)
    val princ = Await.result(authenticator.principals.find("testuser"), Duration.Inf).get
    princ.pass.verify("testpass") should be (true)
    princ.pass.verify("wrongpass") should be (false)
  }
}

