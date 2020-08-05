package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;

/**
 * Any implementing class should meet the following criteria:
 * - Provide JPA named queries findAll and findAllById
 *
 * @author vic on 2018-09-26
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@MappedSuperclass
public abstract class AbstractEntity<T> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = EventConfig.SHA_256, unique = true, nullable = false)
    String sha256;

    @Column(name = EventConfig.ORDERING, nullable = false)
    String ordering;

    @Transient
    private final Class<T> subtype;

    AbstractEntity(Class<T> subtype) {
        this.subtype = subtype;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public T withId(Long id) {
        this.id = id;

        return subtype.cast(this);
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public T withSha256(String sha256) {
        this.sha256 = sha256;
        return subtype.cast(this);
    }

    public String getOrdering() {
        return ordering;
    }

    public void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    public T withOrdering(String ordering) {
        this.ordering = ordering;
        return subtype.cast(this);
    }

    public abstract JsonObject toJson();
}
