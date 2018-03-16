package net.janvsmachine.pgwordvecs

import cats.effect.IO
import doobie.Transactor

object QueryExamples {
  def main(args: Array[String]): Unit = {
    implicit val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql:postgres",
      user = "postgres",
      pass = ""
    )

    val repo = new WordVectorsRepo("glove_6b_50d")
    repo.init()

    // Run some example queries.

    {
      val wordVec: Option[WordVector] = repo.vectorForWord("the")
      println(s"Vector for 'the': $wordVec")

      val mostSimilar: Option[Vector[WordVector]] = wordVec.map(wv => repo.mostSimilarVectors(wv.vector))
      println(s"Most similar vectors for 'the':")
      mostSimilar.foreach(_.foreach(println))
    }

    {
      val wordVec: Option[WordVector] = repo.vectorForWord("fish")
      println(s"Vector for 'fish': $wordVec")

      val mostSimilar: Option[Vector[WordVector]] = wordVec.map(wv => repo.mostSimilarVectors(wv.vector))
      println(s"Most similar vectors for 'fish':")
      mostSimilar.foreach(_.foreach(println))
    }

    {
      val exampleWord = "car"
      println(s"Most similar words for '$exampleWord':")
      repo.mostSimilarWords(exampleWord).foreach(println)
    }

    {
      val exampleWord = "knitting"
      println(s"Most similar words for '$exampleWord':")
      repo.mostSimilarWords(exampleWord).foreach(println)
    }

    def msg(a: String, b: String, c: String): String =
      (for {
        matches <- repo.relatedWords(a, b, c)
        bestMatch <- matches.headOption
        msg = s"$a is to $b as $c is to... $bestMatch\nAll matches: $matches"
      } yield msg)
        .getOrElse("Unable to find related words")

    println(msg("man", "woman", "king"))
    println(msg("man", "woman", "sir"))
    println(msg("man", "woman", "brother"))
    println(msg("norway", "oslo", "france"))
    println(msg("strong", "stronger", "heavy"))
    println(msg("cat", "kitten", "dog"))
  }
}
