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
      val values = Try(str.slice(1, str.length - 1).split(",").map(_.toDouble).toVector)
      values.toOption.map(PgCube.apply)
    }
  }


  object implicits {
    implicit val CubeType: Meta[PgCube] = Meta.other[PGobject]("cube").xmap[PgCube](
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
