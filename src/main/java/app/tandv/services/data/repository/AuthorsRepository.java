package app.tandv.services.data.repository;

import app.tandv.services.data.entity.AuthorEntity;

import java.util.List;

/**
 * @author Vic on 9/1/2018
 **/
public interface AuthorsRepository extends AbstractEntityRepository<AuthorEntity> {
    List<AuthorEntity> findByName(String name);

    List<AuthorEntity> findByNameContaining(String nameLike);
}
