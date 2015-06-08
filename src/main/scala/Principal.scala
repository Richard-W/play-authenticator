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
import play.modules.reactivemongo.ReactiveMongo
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api._
import reactivemongo.bson._
import scala.concurrent.{ Future, ExecutionContext }
import java.lang.reflect._

case class Principal private[authenticator](
  id: String,
  name: String,
  private[authenticator] val pass: PasswordHash,
  private val values: BSONDocument
) {

  private[authenticator] def copy(
      id: String = this.id,
      name: String = this.name,
      pass: PasswordHash = this.pass,
      values: BSONDocument = this.values
  ) = {
    Principal(id, name, pass, values)
  }

  def save()(implicit authenticator: Authenticator): Future[Principal] = {
    authenticator.principals.save(this)
  }

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

  def value[T, B <: BSONValue](key: String, value: T)(implicit writer: BSONWriter[T, B]): Principal = {
    copy(values = values ++ BSONDocument(key -> writer.write(value).asInstanceOf[BSONValue]))
  }

  def cpw(pass: String): Principal = {
    copy(pass = PasswordHash.create(pass))
  }
}

object Principal {

  implicit val bsonReader = new BSONDocumentReader[Principal] {
    def read(bson: BSONDocument): Principal = {
      Principal(
        bson.getAs[BSONObjectID]("_id").get.stringify,
        bson.getAs[String]("name").get,
        bson.getAs[PasswordHash]("pass").get,
        bson.getAs[BSONDocument]("values").get
      )
    }
  }

  implicit val bsonWriter = new BSONDocumentWriter[Principal] {
    def write(princ: Principal): BSONDocument = {
      BSONDocument(
        "_id" -> BSONObjectID(princ.id),
        "name" -> princ.name,
        "pass" -> princ.pass,
        "values" -> princ.values
      )
    }
  }
}
