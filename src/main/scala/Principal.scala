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
import reactivemongo.api._
import reactivemongo.bson._
import scala.concurrent.{ Future, ExecutionContext }
import java.lang.reflect._

case class Principal private[authenticator](
  id: String,
  name: String,
  openid: Option[String],
  private val pass: Option[PasswordHash],
  private val values: BSONDocument
) {

  private[authenticator] def copy(
      id: String = this.id,
      name: String = this.name,
      openid: Option[String] = this.openid,
      pass: Option[PasswordHash] = this.pass,
      values: BSONDocument = this.values
  ) = {
    Principal(id, name, openid, pass, values)
  }

  /** Update the database entry of this principal */
  def save()(implicit authenticator: Authenticator): Future[Principal] = {
    authenticator.principals.save(this)
  }

  /** Get an arbitrary value that is saved with the principal */
  def value[T](key: String)(implicit reader: BSONReader[_ <: BSONValue, T]): Option[T] = {
    values.get(key) flatMap { bson ⇒
      try {
        // This is so damn ugly... only way i found to make it work though
        reader match {
          case r: BSONReader[BSONValue, T]@unchecked ⇒ r.readOpt(bson)
          case _ ⇒ None
        }
      } catch {
        case _: Throwable ⇒ None
      }
    }
  }

  /** Set an arbitrary value to be saved with the principal */
  def value[T, B <: BSONValue](key: String, value: T)(implicit writer: BSONWriter[T, B]): Principal = {
    val elems = values.elements.filter(_._1 != key) :+ (key, writer.write(value))
    copy(values = BSONDocument(elems))
  }

  /** Change the password of the principal */
  def changePassword(pass: String): Principal = {
    copy(pass = Some(PasswordHash.create(pass)))
  }

  /** Change the OpenID of the principal */
  def changeOpenID(openid: String): Principal = {
    copy(openid = Some(openid))
  }

  /** Verify that the given password matches the principals password */
  def verifyPass(password: String): Boolean = (pass map { _.verify(password) }).getOrElse(false)
}

object Principal {

  implicit val bsonReader = new BSONDocumentReader[Principal] {
    def read(bson: BSONDocument): Principal = {
      Principal(
        bson.getAs[BSONObjectID]("_id").get.stringify,
        bson.getAs[String]("name").get,
        (bson.getAs[String]("openid") map { Some(_) }).getOrElse(None),
        (bson.getAs[PasswordHash]("pass") map { Some(_) }).getOrElse(None),
        bson.getAs[BSONDocument]("values").get
      )
    }
  }

  implicit val bsonWriter = new BSONDocumentWriter[Principal] {
    def write(princ: Principal): BSONDocument = {
      BSONDocument(
        "_id" -> BSONObjectID(princ.id),
        "name" -> princ.name,
        "values" -> princ.values
      ) ++ (princ.pass match {
        case Some(pass) ⇒ BSONDocument("pass" -> princ.pass.get)
        case None ⇒ BSONDocument()
      }) ++ (princ.openid match {
        case Some(pass) ⇒ BSONDocument("openid" -> princ.openid.get)
        case None ⇒ BSONDocument()
      })
    }
  }
}
