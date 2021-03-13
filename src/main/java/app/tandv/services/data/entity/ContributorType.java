package app.tandv.services.data.entity;

import java.util.stream.Stream;

/**
 * @author vic on 2020-09-22
 */
public enum ContributorType {
    AUTHOR,
    ILLUSTRATOR,
    EDITOR,
    TRANSLATOR,
    UNDEFINED;

    public static ContributorType fromString(final String name){
        return Stream.of(ContributorType.values())
                .filter(contributorType -> contributorType.name().equals(name))
                .findFirst()
                .orElse(ContributorType.UNDEFINED);
    }
}
