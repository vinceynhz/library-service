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
public final class ContributorData {
    private final String input;
    private final String expectedName;
    private final String expectedOrdering;
    private final String expectedSha256;
    public final boolean expectsBooks;

    private int id;

    public ContributorData(JsonObject fromJsonObject) {
        Assertions.assertTrue(fromJsonObject.containsKey("input"));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.NAME));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.CATALOGUING));
        Assertions.assertTrue(fromJsonObject.containsKey(EventConfig.SHA_256));
        Assertions.assertTrue(fromJsonObject.containsKey("withBooks"));

        this.input = fromJsonObject.getString("input");
        this.expectedName = fromJsonObject.getString(EventConfig.NAME);
        this.expectedOrdering = fromJsonObject.getString(EventConfig.CATALOGUING);
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
                // contributor has all expected values
                .and().body(EventConfig.NAME, hasToString(this.expectedName))
                .and().body(EventConfig.CATALOGUING, hasToString(this.expectedOrdering))
                .and().body(EventConfig.SHA_256, hasToString(this.expectedSha256))
                // and no books are present
                .and().body(EventConfig.CONTRIBUTIONS, hasSize(0));
    }
}
