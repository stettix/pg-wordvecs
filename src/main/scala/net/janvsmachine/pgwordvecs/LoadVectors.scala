package net.janvsmachine.pgwordvecs

/**
  * Command line utility for loading GloVe word vectors into PostgreSQL.
  */
object LoadVectors extends App {

  // TODO:

  // Read needed args, e.g. DB host + post, username & password [optional], schema name, table name.
  // And gloVe file path.

  // TODO: Create schema here if it doesn't exist already? Might be a simple way to do things?

  // Stream the gloVe file (using FS2??) into the DB

  writeRow()

  def writeRow(): Unit = {
    import doobie._
    import doobie.implicits._

    import cats._
    import cats.effect._
    import cats.implicits._

    val program1 = 42.pure[ConnectionIO]

    // A transactor that gets connections from java.sql.DriverManager
    val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql:postgres",
      user = "postgres",
      pass = ""
    )

    val io: IO[Int] = program1.transact(xa)

    val res: Int = io.unsafeRunSync()

    println(s"Ta-da! $res")
  }

}
