package akskscala

object SquerylExample extends App {

  Class.forName("org.h2.Driver")

  def connection: java.sql.Connection = {
    java.sql.DriverManager.getConnection("jdbc:h2:./squerl")
  }

  import org.squeryl._

  SessionFactory.concreteFactory = Option(() => Session.create(connection, new adapters.H2Adapter))

  import org.squeryl.annotations.Column
  case class Book(id: Long, title: String, @Column("author_id") authorId: Long) extends KeyedEntity[Long] {
    lazy val author = DB.authorsToBooks.right(this)
  }
  case class Author(id: Long, @Column("first_name") firstName: String, 
    @Column("middle_name") middleName: Option[String], @Column("last_name") lastName: String) extends KeyedEntity[Long]

  import PrimitiveTypeMode._

  object DB extends Schema {
    val books = table[Book]
    val authors = table[Author]
    val authorsToBooks = oneToManyRelation(authors, books).via((a,b) => a.id === b.authorId)

    // explicit definition example
    on(books) { book => declare(
      book.title is (unique,indexed("book_title_uniq_idx")),
      book.authorId is (indexed("book_author_id_idx"))
    )}
  }

  transaction {
    import DB._
    drop
    create
    printDdl

    authors.insert(Author(1, "Martin", None, "Odersky"))
    authors.insert(Author(2, "David", None, "Pollack"))
    books.insert(Book(1, "Programming in Scala", 1))
    try {
      books.insert(Book(11, "Programming in Scala", 1))
    } catch { 
     case e => println(e.getMessage) // Unique index or primary key violation: "BOOK_TITLE_UNIQ_IDX ON PUBLIC.BOOK(TITLE)";
    }
    books.insert(Book(2, "Beginning Scala", 2))
    books.insert(Book(3, "Neurodiversity in Higher Education", 2))

    // find all
    val allBooks: Iterable[Book] = for (book <- from(books)(b => select(b))) yield book
    allBooks foreach { book => 
      println(book.toString) 
      println(book.author.single.toString) 
    }

    // like query
    for (scalaBook <- from(books)(b => where(b.title.like("%Scala%")).select(b))) println(scalaBook)

  }

}

