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

case class Principal private[authenticator](
  name: String,
  pass: PasswordHash,
  fields: Map[String, String],
  flags: Map[String, Boolean]
) {

  private[authenticator] def copy(
      name: String = this.name,
      pass: PasswordHash = this.pass,
      fields: Map[String, String] = this.fields,
      flags: Map[String, Boolean] = this.flags
  ) = {
    Principal(name, pass, fields, flags)
  }

  def save()(implicit authenticator: Authenticator): Future[Principal] = {
    authenticator.principals.save(this)
  }

  def field(field: String): Option[String] = {
    fields.get(field)
  }

  def field(field: String, value: String): Principal = {
    copy(fields = fields + ((field, value)))
  }

  def flag(flag: String): Option[Boolean] = {
    flags.get(flag)
  }

  def flag(flag: String, value: Boolean): Principal = {
    copy(flags = flags + ((flag, value)))
  }

  def cpw(pass: String): Principal = {
    copy(pass = PasswordHash.create(pass))
  }
}

object Principal {

  implicit val bsonReader = new BSONDocumentReader[Principal] {
    def read(bson: BSONDocument): Principal = {
      Principal(
        bson.getAs[String]("name").get,
        bson.getAs[PasswordHash]("pass").get,
        bson.getAs[Map[String,String]]("fields").get,
        bson.getAs[Map[String,Boolean]]("flags").get
      )
    }
  }

  implicit val bsonWriter = new BSONDocumentWriter[Principal] {
    def write(princ: Principal): BSONDocument = {
      BSONDocument(
        "name" -> princ.name,
        "pass" -> princ.pass,
        "fields" -> princ.fields,
        "flags" -> princ.flags
      )
    }
  }
}
