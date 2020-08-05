package app.tandv.services.util;

import app.tandv.services.configuration.EventConfig;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;

/**
 * @author vic on 2020-08-04
 */
public final class AuthorData {
    private final String input;
    private final String expectedName;
    private final String expectedOrdering;
    private final String expectedSha256;
    public final boolean expectsBooks;

    private int id;

    public AuthorData(JsonObject fromJsonObject) {
        Assertions.assertTrue(fromJsonObject.containsKey("input"));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.NAME));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.ORDERING));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.SHA_256));
        Assertions.assertTrue(fromJsonObject.containsKey("withBooks"));

        this.input = fromJsonObject.getString("input");
        this.expectedName = fromJsonObject.getString(EventConfig.NAME);
        this.expectedOrdering = fromJsonObject.getString(EventConfig.ORDERING);
        this.expectedSha256 = fromJsonObject.getString(EventConfig.SHA_256);
        this.expectsBooks = fromJsonObject.getBoolean("withBooks");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JsonObject toPayload() {
        return new JsonObject().put(EventConfig.NAME, this.input);
    }

    public ValidatableResponse validate(ValidatableResponse response) {
        return response
                // we got an id
                .and().body("$", hasKey(EventConfig.ID))
                // author has all expected values
                .and().body(EventConfig.NAME, hasToString(this.expectedName))
                .and().body(EventConfig.ORDERING, hasToString(this.expectedOrdering))
                .and().body(EventConfig.SHA_256, hasToString(this.expectedSha256))
                // and no books are present
                .and().body(EventConfig.BOOKS, hasSize(0));
    }
}
