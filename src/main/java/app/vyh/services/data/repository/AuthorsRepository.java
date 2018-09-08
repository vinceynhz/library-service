package app.vyh.services.data.repository;

import app.vyh.services.data.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Vic on 9/1/2018
 **/
public interface AuthorsRepository extends JpaRepository<AuthorEntity, Integer> {

    List<AuthorEntity> findByName(String name);

    List<AuthorEntity> findByNameContaining(String nameLike);
}
