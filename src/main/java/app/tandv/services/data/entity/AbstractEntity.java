package app.tandv.services.data.entity;

import io.vertx.core.json.JsonObject;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Any implementing class should meet the following criteria:
 * - Provide JPA named queries findAll and findAllById
 *
 * @author vic on 2018-09-26
 */
@SuppressWarnings("unused")
@MappedSuperclass
public abstract class AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public abstract JsonObject toJson();
}
