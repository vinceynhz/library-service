package app.tandv.services.data.repository;

import app.tandv.services.data.entity.AbstractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Vic on 9/26/2018
 **/
@SuppressWarnings("WeakerAccess")
@NoRepositoryBean
public interface AbstractEntityRepository<E extends AbstractEntity> extends JpaRepository<E, Integer> {
}
