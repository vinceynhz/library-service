package app.tandv.services.data.entity;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Vic on 9/26/2018
 **/
@MappedSuperclass
public abstract class AbstractEntity {
    @Id
    protected Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public abstract Object toResponse();
}
