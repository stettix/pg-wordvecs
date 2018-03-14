package net.janvsmachine.pgwordvecs

import doobie._
import doobie.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats.effect.IO
import doobie.Transactor

import PgCube.implicits._

class WordVectorsRepo(tableName: String)(implicit tx: Transactor.Aux[IO, Unit]) {

  // TODO: make async
  def vectorForWord(word: String): Option[WordVector] = {
    val query = fr"select vector from " ++ Fragment.const(tableName) ++ fr" where word = $word"
    val program = query.query[PgCube].option
    val io = program.transact(tx)
    io.unsafeRunSync().map(cube => WordVector(word, cube.vector))
  }

  def mostSimilarVectors(vector: Vector[Double]): Vector[WordVector] = {
    val inputVector = PgCube(vector)
    val query = fr"select word, vector from " ++ Fragment.const(tableName) ++
      fr" where vector != $inputVector" ++
      fr" order by $inputVector <-> vector limit 10" // TODO: pass in limit or return stream!
    //val query = sql"select vector from glove_6b_50d order by $inputVector <-> vector"
    val program = query.query[(String, PgCube)].to[Vector]
    val io = program.transact(tx)
    io.unsafeRunSync().map { case (word, cube) => WordVector(word, cube.vector) }
  }

  def mostSimilarWords(word: String): Vector[String] = ???

}
