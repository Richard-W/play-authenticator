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
import play.api.inject._
import play.api.inject.guice._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import reactivemongo.bson._
import reactivemongo.bson.DefaultBSONHandlers._

class AuthenticatorModuleSpec extends FlatSpec with Matchers with BeforeAndAfter with MongoEmbedDatabase {

  val mongoURI = "mongodb://localhost:12345/test"
  var application: Application = null
  var injector: Injector = null

  var mongoProps: MongodProps = null

  before {
    application = new GuiceApplicationBuilder()
      .configure("mongo.uri" -> mongoURI)
      .bindings(new ReactiveMongoModule)
      .bindings(new AuthenticatorModule)
      .build
    injector = application.injector
    mongoProps = mongoStart()
  }

  after {
    Await.result(application.stop, Duration.Inf)
    mongoStop(mongoProps)
  }

  "AuthenticatorModule" should "supply a working Authenticator" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    Await.result(authenticator.principals.create("testuser", "testpass"), 5.seconds)
    val princ = Await.result(authenticator.principals.findByName("testuser"), 5.seconds).get
    princ.verifyPass("testpass") should be (true)
    princ.verifyPass("wrongpass") should be (false)
  }

  "A Principal" should "keep its id when updated" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    Await.result(authenticator.principals.create("testuser2", "testpass"), 5.seconds)
    val princ1 = Await.result(authenticator.principals.findByName("testuser2"), 5.seconds).get
    Await.result(princ1.value("foo", "bar").save, Duration.Inf)
    val princ2 = Await.result(authenticator.principals.findByName("testuser2"), 5.seconds).get

    princ1.id should be (princ2.id)
    princ1.value[String]("foo").isDefined should be (false)
    princ2.value[String]("foo").get should be ("bar")
  }

  it should "be retrievable by its name and id" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    val name = "testuser3"
    val id = Await.result(authenticator.principals.create(name, "testpass"), 5.seconds).get.id
    val princ1 = Await.result(authenticator.principals.findByName(name), 5.seconds).get
    val princ2 = Await.result(authenticator.principals.findByID(id), 5.seconds).get

    princ1.id should be (id)
    princ2.name should be (name)
  }

  it should "be able to save arbitrary values" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    val name = "testuser4"
    val id = Await.result(authenticator.principals.create(name, "testpass"), 5.seconds).get.id
    val princ1 = Await.result(authenticator.principals.findByID(id), 5.seconds).get
    princ1.value("test", true).value("str", "test").save
    val princ2 = Await.result(authenticator.principals.findByID(id), 5.seconds).get
    princ2.value[Boolean]("test").get should be (true)
    princ2.value[String]("str").get should be ("test")
    princ2.value[Boolean]("str") should be (None)
  }

  it should "be able to cleanly update values" in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    val name = "testuser4"
    val id = Await.result(authenticator.principals.create(name, "testpass", BSONDocument("key1" -> "v1", "key2" -> "v2")), 5.seconds).get.id
    val princ1 = Await.result(authenticator.principals.findByID(id), 5.seconds).get
    Await.result(princ1.value("key1", "v3").save, 5.seconds)
    val princ2 = Await.result(authenticator.principals.findByID(id), 5.seconds).get
    princ2.value[String]("key1").get should be ("v3")
    princ2.value[String]("key2").get should be ("v2")
  }

  "The PrincipalController" should "be able to find all principals." in {
    implicit val authenticator = injector.instanceOf[Authenticator]
    Await.result(authenticator.principals.create("a1", "testpass"), 5.seconds)
    Await.result(authenticator.principals.create("a2", "testpass"), 5.seconds)
    Await.result(authenticator.principals.create("a3", "testpass"), 5.seconds)

    val allPrincs = Await.result(authenticator.principals.findAll, 5.seconds) map { princ ⇒ princ.name }
    allPrincs.contains("a1") should be (true)
    allPrincs.contains("a2") should be (true)
    allPrincs.contains("a3") should be (true)
  }
}

