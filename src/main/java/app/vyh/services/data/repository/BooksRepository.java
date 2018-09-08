package app.vyh.services.data.repository;

import app.vyh.services.data.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Vic on 8/29/2018
 **/
public interface BooksRepository extends JpaRepository<BookEntity, Integer> {

    List<BookEntity> findByTitle(String title);

    List<BookEntity> findByTitleContaining(String titleLike);
}
