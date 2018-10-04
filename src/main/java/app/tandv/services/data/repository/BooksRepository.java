package app.tandv.services.data.repository;

import app.tandv.services.data.entity.BookEntity;

import java.util.List;

/**
 * @author Vic on 8/29/2018
 **/
public interface BooksRepository extends AbstractEntityRepository<BookEntity> {
    List<BookEntity> findByTitle(String title);

    List<BookEntity> findByTitleContaining(String titleLike);
}
