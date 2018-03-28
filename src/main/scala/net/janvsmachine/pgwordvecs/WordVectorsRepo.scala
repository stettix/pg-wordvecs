package net.janvsmachine.pgwordvecs

import java.nio.file.Path

import breeze.linalg.{norm, DenseVector}
import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import net.janvsmachine.pgwordvecs.PgCube.implicits._

import scala.io.{Codec, Source}

class WordVectorsRepo(tableName: String)(implicit xa: Transactor.Aux[IO, Unit]) {

  import WordVectorsRepo._

  def init(): Unit = {
    val createTable =
      (fr"create table if not exists " ++ Fragment.const(tableName) ++
        fr""" (
          word text primary key,
          vector cube not null
         )""").update.run
    val createIndex =
      (fr"create index if not exists " ++ Fragment.const(tableName + "_idx") ++
        fr" on " ++ Fragment.const(tableName) ++
        fr" using gist (vector)").update.run

    val ops = (createTable, createIndex).mapN(_ + _)

    val r: Int = ops.transact(xa).unsafeRunSync
    println(s"Create table and index result: $r")
  }

  def loadFromFile(path: Path): Unit = {
    println(s"Reading from file $path")

    // TODO: Stream this instead of reading it into memory, and update in batches?
    // Then again, this brute force implementations works OK...
    val lines = Source.fromFile(path.toFile)(Codec.UTF8).getLines()
    val parsed: List[WordVector] = lines.map(parseGloveLine).toList
    println(s"Parsed ${parsed.size} word vectors")

    val normalised: List[WordVector] = parsed.map(x => x.copy(vector = normalise(x.vector)))

    val ops: List[Update0] = normalised.map(insertOp)
    val combined: ConnectionIO[List[Int]] = ops.traverse(_.run)
    val res: Seq[Int] = combined.transact(xa).unsafeRunSync

    println("Results: " + res.distinct)
  }

  def normalise(v: Vector[Double]): Vector[Double] = {
    val dv = DenseVector(v.toArray)
    (dv / norm(dv)).toScalaVector()
  }

  def insert(wv: WordVector): ConnectionIO[Int] = {
    val cube = PgCube(wv.vector)
    val query = fr"insert into " ++ Fragment.const(tableName) ++
      fr""" (word, vector) values (${wv.word}, $cube)
            on conflict do nothing"""
    query.update.run
  }

  // TODO: remove the duplication with the above
  private def insertOp(wv: WordVector): Update0 = {
    val cube = PgCube(wv.vector)
    val query = fr"insert into " ++ Fragment.const(tableName) ++
      fr""" (word, vector) values (${wv.word}, $cube)
            on conflict do nothing"""
    query.update
  }

  // TODO: make async
  def vectorForWord(word: String): Option[WordVector] = {
    val query = fr"select vector from " ++ Fragment.const(tableName) ++ fr" where word = $word"
    val program = query.query[PgCube].option
    val io = program.transact(xa)
    io.unsafeRunSync().map(cube => WordVector(word, cube.vector))
  }

  def mostSimilarVectors(vector: Vector[Double]): Vector[WordVector] = {
    val inputVector = PgCube(vector)
    val query = fr"select word, vector from " ++ Fragment.const(tableName) ++
      fr" where vector != $inputVector" ++
      fr" order by $inputVector <-> vector limit 10" // TODO: pass in limit or return stream!
    val program = query.query[(String, PgCube)].to[Vector]
    val io = program.transact(xa)
    io.unsafeRunSync().map { case (word, cube) => WordVector(word, cube.vector) }
  }

  def mostSimilarWords(word: String): Vector[String] = {
    val query = fr"select word from " ++ Fragment.const(tableName) ++
      fr" order by (select vector from " ++ Fragment.const(tableName) ++
      fr" where word = $word) <-> vector limit 10"

    val program = query.query[String].to[Vector]
    val io = program.transact(xa)

    io.unsafeRunSync()
  }

  /**
    * Returns the words that has the most similar relations to word C as word A has to word B.
    * In other words, the query is:
    * 'Word A is to word B as word C is to what?"
    */
  def relatedWords(wordA: String, wordB: String, wordC: String): Option[Vector[String]] =
    for {
      vecA <- vectorForWord(wordA)
      vecB <- vectorForWord(wordB)
      vecC <- vectorForWord(wordC)
      diff = (vecB.vector zip vecA.vector).map { case (x, y) => x - y } // Not a very efficient vector operation but fine for this...
      targetVec = (vecC.vector zip diff).map { case (x, y) => x + y } // Ditto
    } yield mostSimilarVectors(targetVec).map(_.word).filterNot(Set(wordA, wordB, wordC).contains)
}

object WordVectorsRepo {

  private[pgwordvecs] def parseGloveLine(line: String): WordVector = {
    val fields = line.split("\\s").toVector
    val (word, values) = (fields.head, fields.drop(1))
    val vector = values.map(_.toDouble)
    WordVector(word, vector)
  }

}
