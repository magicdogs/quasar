/*
 * Copyright 2014–2017 SlamData Inc.
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

package quasar.physical.marklogic.xquery

import quasar.Predef._
import quasar.physical.marklogic.xquery.syntax._

import monocle.macros.Lenses
import scalaz._
import scalaz.syntax.show._

@Lenses
final case class DefaultCollationDecl(collation: Collation) {
  def render: String = s"declare default collation ${collation.value.get.xs.shows}"
}

object DefaultCollationDecl {
  implicit val order: Order[DefaultCollationDecl] =
    Order.orderBy(_.collation)

  implicit val show: Show[DefaultCollationDecl] =
    Show.shows(nd => s"DefaultCollationDecl(${nd.render})")
}
