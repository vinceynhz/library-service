DROP TABLE IF EXISTS BOOKS;
DROP TABLE IF EXISTS AUTHORS;
DROP TABLE IF EXISTS BOOKS_AUTHORS;
DROP INDEX IF EXISTS IX_BOOKS_AUTHORS;

CREATE CACHED TABLE BOOKS (
  id             INT          NOT NULL PRIMARY KEY,
  title          VARCHAR(500) NOT NULL,
  ordering_title VARCHAR(500) NOT NULL,
  isbn           VARCHAR(20)  NOT NULL,
  year           VARCHAR(4)   NOT NULL,
  format         VARCHAR(2)   NOT NULL,
  quantity       TINYINT      DEFAULT 1,
);

CREATE CACHED TABLE AUTHORS (
  id            INT          NOT NULL PRIMARY KEY,
  name          VARCHAR(255) NOT NULL,
  ordering_name VARCHAR(255) NOT NULL,
  initials      VARCHAR(2)   NOT NULL,
);

-- This table will contain the cross reference between books and authors; i.e which authors are in a book, which books
-- were written by given author. We have a primary key on both id's (books and authors) which indexes both columns and
-- the first column, and additionally we add a separate index below for the second column
CREATE CACHED TABLE BOOKS_AUTHORS (
  author_id INT NOT NULL,
  book_id   INT NOT NULL,
--   PRIMARY KEY (author_id, book_id),
  FOREIGN KEY (author_id) REFERENCES AUTHORS (id),
  FOREIGN KEY (book_id) REFERENCES BOOKS (id),
);

-- CREATE INDEX IX_BOOKS_AUTHORS
--   ON BOOKS_AUTHORS (book_id);

-- select b.*
-- from books b, authors a, books_by_author ba
-- where a.name like '%stephen%king%'
-- and ba.author_id = a.id
-- and ba.book_id = b.id
-- order by b.year;
--
--
-- select * from BOOKS_BY_AUTHOR where book_id = 12345;