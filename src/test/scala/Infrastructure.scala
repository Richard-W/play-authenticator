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
import play.api._
import play.api.inject.guice._
import play.modules.reactivemongo._
import scala.concurrent.Await
import scala.concurrent.duration._
import de.flapdoodle.embed.mongo.distribution.Version

trait AuthenticatorSpec extends FlatSpec with Matchers with MongoEmbedDatabase {
  implicit var application: Application = null 
  def injector = if(application != null) application.injector else null

  private var mongoProps: MongodProps = null

  override def withFixture(test: NoArgTest) = {
    mongoProps = mongoStart(version = Version.V2_6_1)
    application = new GuiceApplicationBuilder()
      .configure("mongodb.servers" -> Seq("localhost:12345"))
      .configure("mongodb.db" -> "test")
      .bindings(new ReactiveMongoModule)
      .bindings(new AuthenticatorModule)
      .build
    try {
      super.withFixture(test)
    } finally {
      Await.result(application.stop, Duration.Inf)
      mongoStop(mongoProps)
    }
  }
}
