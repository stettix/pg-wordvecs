package net.janvsmachine.pgwordvecs

import net.janvsmachine.pgwordvecs.PgCube.fromString
import org.scalatest.FlatSpec

class PgCubeSpec extends FlatSpec {

  "Parsing a cube" should "parse valid cubes correctly" in {
    assert(fromString("(1)").contains(PgCube(1.toDouble)))
    assert(fromString("(1.2)").contains(PgCube(1.2d)))
    assert(fromString("(1,2)").contains(PgCube(1.toDouble, 2.toDouble)))
    assert(fromString("(1.1,2.2)").contains(PgCube(1.1d, 2.2d)))
    assert(fromString("(1.1,2.2,3.3,4.4)").contains(PgCube(1.1d, 2.2d, 3.3d, 4.4d)))
  }

  it should "return None for invalid strings" in {
    val invalidStrings = Seq(" ",
      "(",
      ")",
      "(1",
      "(1.",
      "(1.1",
      "(1,2",
      "(1,2))",
      "((1,2)",
      "foo(1, 2, 3)")

    invalidStrings.foreach { str =>
      withClue(s"Input: '$str': ") {
        assert(fromString(str).isEmpty)
      }
    }
  }

  "Rendering a cube" should "render all cube values" in {
    assert(PgCube().render == "()")
    assert(PgCube(0).render == "(0.0)")
    assert(PgCube(0.0).render == "(0.0)")
    assert(PgCube(1, 2, 3).render == "(1.0,2.0,3.0)")
    assert(PgCube(1.1d, 2.2d, 3.3d).render == "(1.1,2.2,3.3)")
  }

}
