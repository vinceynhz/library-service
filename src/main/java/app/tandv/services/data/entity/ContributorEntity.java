package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.util.EntityUtils;
import app.tandv.services.util.StringUtils;
import app.tandv.services.util.collections.FluentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Notes on contributor entities:
 *
 * <strong>Normalization</strong>
 * Contributor names are going to be normalized according to the rules of title casing defined in
 * {@link StringUtils#titleCase(String)}.
 *
 * <strong>Ordering</strong>
 * The string used for alphabetical cataloguing will be determined according to the rules defined in
 * {@link StringUtils#contributorForOrdering(String)}.
 * <p>
 * Please note that two or more contributors may yield the same cataloguing value:
 * - Diane Maxwell
 * - Dr. Diane Maxwell
 * - Diane Maxwell Jr.
 * - Diane Maxwell III
 * <p>
 * All above names would be ordered as <em>maxwell diane</em> once all honorifics, special characters
 * and roman numerals are removed.
 *
 * <strong>Uniqueness</strong>
 * A contributor uniqueness is determined by the {@link StringUtils#sha256(String)} function over the
 * normalized version of the contributor's name as described above.
 *
 * <p>
 * The case for homonym contributors is still to be determined.
 *
 * @author vic on 2018-08-31
 */
@SuppressWarnings({"unused", "WeakerAccess", "JpaQlInspection"})
@Entity
@Table(name = "contributor")
@NamedQueries({
        @NamedQuery(
                name = "ContributorEntity.findAll",
                query = "SELECT DISTINCT a FROM ContributorEntity a LEFT OUTER JOIN FETCH a.contributions"
        ),
        @NamedQuery(
                name = "ContributorEntity.findAllById",
                query = "SELECT DISTINCT a FROM ContributorEntity a WHERE a.id IN :ids"
        ),
        @NamedQuery(
                name = "ContributorEntity.findById",
                query = "SELECT DISTINCT a FROM ContributorEntity a WHERE a.id = :id"
        )
})
public class ContributorEntity extends LibraryEntity<ContributorEntity> {
    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.NAME);

    @Column(name = EventConfig.NAME, nullable = false)
    private String name;

    @OneToMany(mappedBy = "contributor")
    private Set<BookContributor> contributions;

    public ContributorEntity() {
        super(ContributorEntity.class);
    }

    public static ContributorEntity fromJson(JsonObject data) throws IllegalArgumentException {
        if (!data.fieldNames().containsAll(REQUIRED_FIELDS)) {
            throw new IllegalArgumentException("Not enough attributes provided. Required: " + REQUIRED_FIELDS.toString());
        }
        String rawName = data.getString(EventConfig.NAME);
        String normalized = StringUtils
                .titleCase(rawName, true) // for contributor names we need to handle upper case
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate title case for name [" + rawName + "]"));
        String ordering = StringUtils
                .contributorForOrdering(rawName)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate cataloguing string for name [" + rawName + "]"));
        String sha256 = StringUtils
                .sha256(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate SHA 256 for name [" + normalized + "]"));

        return new ContributorEntity()
                .withGeneratedId()
                .withName(normalized)
                .withCataloguing(ordering)
                .withSha256(sha256);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContributorEntity withName(String name) {
        this.name = name;
        return this;
    }

    public Set<BookContributor> getContributions() {
        if (this.contributions == null) {
            this.contributions = new HashSet<>();
        }
        return contributions;
    }

    public void addContribution(BookContributor contribution) {
        this.getContributions().add(contribution);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContributorEntity that = (ContributorEntity) o;
        return Objects.equals(this.sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(this.sha256);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                EventConfig.ID + "=" + this.id +
                ", " + EventConfig.SHA_256 + "='" + this.sha256 + '\'' +
                ", " + EventConfig.NAME + "='" + this.name + '\'' +
                ", " + EventConfig.CATALOGUING + "='" + this.cataloguing + '\'' +
                ", " + EventConfig.CONTRIBUTIONS + "=" + this.contributions.size() +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonArray contributions = this.getContributions().stream()
                .map(BookContributor::toContributionJson)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        return new JsonObject()
                .put(EventConfig.ID, this.id)
                .put(EventConfig.SHA_256, this.sha256)
                .put(EventConfig.NAME, this.name)
                .put(EventConfig.CATALOGUING, this.cataloguing)
                .put(EventConfig.CONTRIBUTIONS, contributions)
                ;
    }
}
