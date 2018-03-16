package net.janvsmachine.pgwordvecs

import java.nio.file.Path

import doobie._
import doobie.implicits._
import cats._
import cats.syntax._
import cats.data._
import cats.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats.effect.IO
import PgCube.implicits._

import scala.io.{Codec, Source}

class WordVectorsRepo(tableName: String)(implicit xa: Transactor.Aux[IO, Unit]) {

  import WordVectorsRepo._

  private val table = Fragment.const(tableName)

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

    // TODO: Stream this instead, and update in batches? Then again, this brute force implemenetations works fine...
    val lines = Source.fromFile(path.toFile)(Codec.UTF8).getLines()
    val parsed: List[WordVector] = lines.map(parseGloveLine).toList
    println(s"Parsed ${parsed.size} word vectors")

    val ops: List[Update0] = parsed.map(insertOp)
    val combined: ConnectionIO[List[Int]] = ops.traverse(_.run)
    val res: Seq[Int] = combined.transact(xa).unsafeRunSync

    println("Results: " + res.distinct)
  }

  def insert(wv: WordVector): ConnectionIO[Int] = {
    val cube = PgCube(wv.vector)
    val query = fr"insert into " ++ Fragment.const(tableName) ++ fr" (word, vector) values (${wv.word}, $cube) on conflict do nothing"
    query.update.run
  }

  private def insertOp(wv: WordVector): Update0 = {
    val cube = PgCube(wv.vector)
    val query = fr"insert into " ++ Fragment.const(tableName) ++
      fr""" (word, vector) values (${wv.word}, $cube)
            on conflict do nothing""".stripMargin
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

  def mostSimilarWords(word: String): Vector[String] = ???

}

object WordVectorsRepo {

  private[pgwordvecs] def parseGloveLine(line: String): WordVector = {
    val fields = line.split("\\s").toVector
    val (word, values) = (fields.head, fields.drop(1))
    val vector = values.map(_.toDouble)
    WordVector(word, vector)
  }

}
