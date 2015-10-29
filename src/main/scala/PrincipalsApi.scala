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
import play.modules.reactivemongo._
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import akka.actor.ActorSystem
import scala.util.{ Try, Success, Failure }

trait PrincipalsApi {

  /** Create a principal with a password */
  def createWithPassword(name: String, password: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]]

  /** Create a principal */
  def createWithOpenID(name: String, openid: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]]

  /** Retrieve a principal by its name from the database */
  def findByName(name: String): Future[Option[Principal]]

  /** Retrieve a principal by its id from the database */
  def findByID(id: String): Future[Option[Principal]]

  /** Retrieve a principal by its openid */
  def findByOpenID(openid: String): Future[Option[Principal]]

  /** Get all registered principals */
  def findAll: Future[Seq[Principal]]

  /** Save a principal back to the database */
  def save(princ: Principal): Future[Principal]
}

private[authenticator] class PrincipalsApiImpl @Inject()(
    conf: Configuration,
    mongo: ReactiveMongoApi,
    actorSystem: ActorSystem
) extends PrincipalsApi {

  /* Import execution context */
  import actorSystem.dispatcher

  val collectionName = conf.getString("authenticator.principalCollection") match {
    case Some(collection) ⇒ collection
    case None ⇒ "authenticatorPrincipals"
  }

  def collection = mongo.db.collection[BSONCollection](collectionName)

  /* Ensure indizes are set correctly */
  val indexManager = collection.indexesManager
  indexManager.ensure(indexes.Index(Seq(("name", indexes.IndexType.Text)), unique = true)) map {
    case false ⇒ throw new Exception("Can not set index for users")
    case true ⇒
  }
  indexManager.ensure(indexes.Index(Seq(("openid", indexes.IndexType.Text)), unique = true)) map {
    case false ⇒ throw new Exception("Can not set index for users")
    case true ⇒
  }

  def createWithPassword(name: String, password: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]] = {
    val princ = Principal(BSONObjectID.generate.stringify, name, None, Some(PasswordHash.create(password)), values)
    collection.insert(princ) flatMap { lastError ⇒
      if(lastError.ok) findByID(princ.id) map { opt ⇒ Success(opt.get) }
      else Future.successful(Failure(lastError))
    }
  }

  def createWithOpenID(name: String, openid: String, values: BSONDocument = BSONDocument()): Future[Try[Principal]] = {
    val princ = Principal(BSONObjectID.generate.stringify, name, Some(openid), None, values)
    collection.insert(princ) flatMap { lastError ⇒
      if(lastError.ok) findByID(princ.id) map { opt ⇒ Success(opt.get) }
      else Future.successful(Failure(lastError))
    }
  }

  def findByName(name: String): Future[Option[Principal]] = {
    collection.find(BSONDocument("name" -> name)).cursor[Principal].collect[Seq]() map { seq ⇒
      if(seq.length > 0) Some(seq(0))
      else None
    }
  }

  def findByID(id: String): Future[Option[Principal]] = {
    collection.find(BSONDocument("_id" -> BSONObjectID(id))).cursor[Principal].collect[Seq]() map { seq ⇒
      if(seq.length > 0) Some(seq(0))
      else None
    }
  }

  def findByOpenID(openid: String): Future[Option[Principal]] = {
    collection.find(BSONDocument("openid" -> BSONString(openid))).cursor[Principal].collect[Seq]() map { seq ⇒
      if(seq.length > 0) Some(seq(0))
      else None
    }
  }

  def findAll: Future[Seq[Principal]] = {
    collection.find(BSONDocument()).cursor[Principal].collect[Seq]()
  }

  def save(princ: Principal): Future[Principal] = {
    collection.update(BSONDocument("name" -> princ.name), princ) map { lastError ⇒
      if(lastError.ok) {
        princ
      } else {
        throw lastError
      }
    }
  }
}

