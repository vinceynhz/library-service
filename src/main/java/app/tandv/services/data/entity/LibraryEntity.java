package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;

/**
 * This class represents an record of an entity in the library database with the following characteristics:
 * - The data in the record is unique (not two records of similar data can be created)
 * - The data in the record can be sorted for cataloguing
 *
 * Uniqueness and cataloguing rules must be defined by each implementing class.
 *
 * Any implementing class should meet the following criteria:
 * - Provide JPA named queries findAll and findAllById
 *
 * @author vic on 2018-09-26
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@MappedSuperclass
public abstract class LibraryEntity<T> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = EventConfig.SHA_256, unique = true, nullable = false)
    String sha256;

    @Column(name = EventConfig.CATALOGUING, nullable = false)
    String cataloguing;

    @Transient
    private final Class<T> subtype;

    LibraryEntity(Class<T> subtype) {
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

    public String getCataloguing() {
        return cataloguing;
    }

    public void setCataloguing(String cataloguing) {
        this.cataloguing = cataloguing;
    }

    public T withCataloguing(String ordering) {
        this.cataloguing = ordering;
        return subtype.cast(this);
    }

    public abstract JsonObject toJson();
}
