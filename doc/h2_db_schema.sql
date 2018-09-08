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

CREATE CACHED TABLE BOOKS_AUTHORS (
  author_id INT NOT NULL,
  book_id   INT NOT NULL,
  FOREIGN KEY (author_id) REFERENCES AUTHORS (id),
  FOREIGN KEY (book_id) REFERENCES BOOKS (id),
);

-- THIS QUERY IS PROVIDED TO CHECK THE CROSS REFERENCE ONCE BOOKS ARE LOADED
SELECT
       B.id as BOOK_ID,
       B.title as TITLE,
       B.isbn as ISBN,
       B.quantity as QUANTITY,
       B.format as FORMAT,
       A.name as AUTHOR_NAME,
       A.id as AUTHOR_ID
FROM BOOKS B
INNER JOIN BOOKS_AUTHORS BA on b.id = BA.book_id
INNER JOIN AUTHORS A on BA.author_id = A.id
GROUP BY A.id
ORDER BY B.ordering_title