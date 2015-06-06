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

import reactivemongo.bson._

object `package` {

  implicit val bsonStringMapReader = new BSONDocumentReader[Map[String, String]] {
    def read(bson: BSONDocument): Map[String, String] = {
      bson.elements.toMap map { case (key, value) ⇒
        (key, value.asInstanceOf[BSONString].value)
      }
    }
  }

  implicit val bsonStringMapWriter = new BSONDocumentWriter[Map[String, String]] {
    def write(map: Map[String, String]): BSONDocument = {
      BSONDocument(map map { case (key, value) ⇒ (key, BSONString(value)) })
    }
  }

  implicit val bsonBoolMapReader = new BSONDocumentReader[Map[String, Boolean]] {
    def read(bson: BSONDocument): Map[String, Boolean] = {
      bson.elements.toMap map { case (key, value) ⇒
        (key, value.asInstanceOf[BSONBoolean].value)
      }
    }
  }

  implicit val bsonBoolMapWriter = new BSONDocumentWriter[Map[String, Boolean]] {
    def write(map: Map[String, Boolean]): BSONDocument = {
      BSONDocument(map map { case (key, value) ⇒ (key, BSONBoolean(value)) })
    }
  }
}
