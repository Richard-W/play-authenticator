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

import scala.concurrent.Future
import play.api._
import javax.inject._
import play.modules.reactivemongo.ReactiveMongo
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import akka.actor.ActorSystem
import scala.util.{ Try, Success, Failure }

trait PrincipalController {
  /** Name of the principal collection */
  def principalCollection: String

  /** Create a principal */
  def create(name: String, password: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]]

  /** Retrieve a principal by its name from the database */
  def findByName(name: String): Future[Option[Principal]]

  /** Retrieve a principal by its id from the database */
  def findByID(id: String): Future[Option[Principal]]

  /** Save a principal back to the database */
  def save(princ: Principal): Future[Principal]
}

final class PrincipalControllerImpl @Inject()(
    conf: Configuration,
    mongo: ReactiveMongo,
    actorSystem: ActorSystem
) extends PrincipalController {

  /* Import execution context */
  import actorSystem.dispatcher

  val principalCollection = conf.getString("authenticator.principalCollection") match {
    case Some(collection) ⇒ collection
    case None ⇒ "authenticatorPrincipals"
  }

  /* Ensure indizes are set correctly */
  mongo.db.collection[BSONCollection](principalCollection)
      .indexesManager
      .ensure(indexes.Index(Seq(("name", indexes.IndexType.Text)), unique = true)) map {
    case false ⇒ throw new Exception("Can not set index for users")
    case true ⇒
  }

  def create(name: String, password: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]] = {
    val collection = mongo.db.collection[BSONCollection](principalCollection)
    val princ = Principal(BSONObjectID.generate.stringify, name, PasswordHash.create(password), values)
    collection.insert(princ) flatMap { lastError ⇒
      if(lastError.ok) findByID(princ.id) map { opt ⇒ Success(opt.get) }
      else Future.successful(Failure(lastError))
    }
  }

  def findByName(name: String): Future[Option[Principal]] = {
    val collection = mongo.db.collection[BSONCollection](principalCollection)
    collection.find(BSONDocument("name" -> name)).cursor[Principal].collect[Seq]() map { seq ⇒
      if(seq.length > 0) Some(seq(0))
      else None
    }
  }

  def findByID(id: String): Future[Option[Principal]] = {
    val collection = mongo.db.collection[BSONCollection](principalCollection)
    collection.find(BSONDocument("_id" -> BSONObjectID(id))).cursor[Principal].collect[Seq]() map { seq ⇒
      if(seq.length > 0) Some(seq(0))
      else None
    }
  }

  def save(princ: Principal): Future[Principal] = {
    val collection = mongo.db.collection[BSONCollection](principalCollection)
    collection.update(BSONDocument("name" -> princ.name), princ) map { lastError ⇒
      if(lastError.ok) {
        princ
      } else {
        throw lastError
      }
    }
  }
}

