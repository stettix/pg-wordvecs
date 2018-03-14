package net.janvsmachine.pgwordvecs

import doobie.Meta
import org.postgresql.util.PGobject

import scala.util.Try

case class PgCube(vector: Vector[Double]) {
  def render: String = vector.mkString("(", ",", ")")
}

object PgCube {

  def apply(values: Double*): PgCube = PgCube(values.toVector)

  def fromString(str: String): Option[PgCube] = {
    // A noddy bit of parsing for now...
    if (str.isEmpty || str.head != '(' || str.charAt(str.length - 1) != ')')
      None
    else {
      val values = Try(
        str.slice(1, str.length - 1)
          .split(",")
          .map(_.toDouble
          ).toVector)
      values.toOption.map(PgCube.apply)
    }
  }

  object implicits {

    // Register custom mapping for 'cube' type from postgres 'cube' extension.
    // See https://github.com/tpolecat/doobie/blob/series/0.5.x/modules/postgres/src/main/scala/doobie/postgres/Instances.scala#L54-L63
    // for a similar example in the Doobie codebase.
    implicit val CubeType: Meta[PgCube] = Meta.other[PGobject]("cube").xmap[PgCube](
      // TODO: Apparently the null checking in these two functions isn't necessary
      o => Option(o).flatMap(a => PgCube.fromString(a.getValue)).orNull, // PgObject => Cube
      a => Option(a).map { a => // Cube => PgObject
        val o = new PGobject
        o.setType("cube")
        o.setValue(a.render)
        o
      }.orNull
    )

  }

}
