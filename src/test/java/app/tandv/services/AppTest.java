package app.tandv.services;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.configuration.MediaTypes;
import app.tandv.services.data.entity.BookFormat;
import app.tandv.services.exception.PartialResultException;
import app.tandv.services.util.AuthorData;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import java.io.*;
import java.net.ServerSocket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit test for simple App.
 *
 * @author Vic on 8/28/2018
 */
@TestMethodOrder(OrderAnnotation.class)
class AppTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppTest.class);

    private static App app;

    private static List<AuthorData> authors;
    private static List<JsonObject> books;

    @BeforeAll
    static void init() throws IOException {
        LOGGER.info("Booting up test application");

        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        LOGGER.info("Port obtained: {}", port);

        RestAssured.port = port;
        app = new DummyApp(port);
        app.startUp();

        await().atMost(30, TimeUnit.SECONDS).until(app::isStarted);

        LOGGER.info("Loading test data");
        // Author data
        authors = new JsonArray(AppTest.loadStringResource("authorData.json"))
                .stream()
                .filter(obj -> JsonObject.class.isAssignableFrom(obj.getClass()))
                .map(JsonObject.class::cast)
                .map(AuthorData::new)
                .collect(Collectors.toList());

        books = Stream.of(
                "The Devil's Trap", // authors[0]
                "Fool's Birthright", // authors[0,1]
                "Electric Apocalypse", // authors[2]
                "The Secret of the Vanishing Baker", // authors[3]
                "Death of the Singing Monkey") // authors[2,3]
                .map(title -> new JsonObject()
                        .put(EventConfig.TITLE, title)
                        .put(EventConfig.FORMAT, BookFormat.PAPERBACK.name())
                        .put(EventConfig.AUTHORS, new JsonArray())
                )
                .collect(Collectors.toList());
    }

    @AfterAll
    static void shutdown() {
        int rc = app.close();
        Assertions.assertEquals(0, rc, "Error closing application");
        LOGGER.info("Finished testing application");
    }

    @Test
    @Order(1)
    void testQueryNoData() {
        LOGGER.info("\nTEST QUERY EMPTY DATABASE ===========================================");
        RestAssured.get("/data/authors")
                .then().assertThat()
                .statusCode(HttpResponseStatus.OK.code())
                .and().body("$", hasSize(0));
        RestAssured.get("/data/books")
                .then().assertThat()
                .statusCode(HttpResponseStatus.OK.code())
                .and().body("$", hasSize(0));
    }

    @Test
    @Order(2)
    void testAddAuthors() {
        LOGGER.info("\nTEST ADD AUTHORS ====================================================");
        for (AuthorData testCase : authors) {
            JsonObject payload = testCase.toPayload();
            // We add one author
            ValidatableResponse response = request()
                    .body(payload.encode())
                    .post("/data/author")
                    .then().assertThat()
                    .statusCode(HttpResponseStatus.CREATED.code());
            testCase.setId(
                    testCase.validate(response)
                            .extract().body()
                            .jsonPath().getInt(EventConfig.ID)
            );
        }
    }

    @Test
    @Order(3)
    void testUpdateBookData() {
        LOGGER.info("\nTEST UPDATE BOOK DATA ===============================================");
        // Now that we got authors loaded in the DB and we know their IDs we can populate them into our test data
        List<Integer> authorIds = authors
                .stream()
                .map(AuthorData::getId)
                .collect(Collectors.toList());

        books.get(0).getJsonArray(EventConfig.AUTHORS).add(authorIds.get(0));
        books.get(1).getJsonArray(EventConfig.AUTHORS).add(authorIds.get(0)).add(authorIds.get(1));
        books.get(2).getJsonArray(EventConfig.AUTHORS).add(authorIds.get(2));
        books.get(3).getJsonArray(EventConfig.AUTHORS).add(authorIds.get(3));
        books.get(4).getJsonArray(EventConfig.AUTHORS).add(authorIds.get(2)).add(authorIds.get(3));

        books.forEach(book -> Assertions.assertFalse(book.getJsonArray(EventConfig.AUTHORS).isEmpty()));
    }

    @Test
    @Order(4)
    void testAddBooks() {
        LOGGER.info("\nTEST ADD BOOKS ======================================================");
        String title;
        for (JsonObject payload : books) {
            title = payload.getString(EventConfig.TITLE);
            String responseBody = request()
                    .body(payload.encode())
                    .post("/data/book")
                    .then().assertThat()
                    .statusCode(HttpResponseStatus.CREATED.code())
                    // title is present
                    .and().body(EventConfig.TITLE, hasToString(title))
                    // authors match what we sent
                    .and().body(EventConfig.AUTHORS, hasSize(payload.getJsonArray(EventConfig.AUTHORS).size()))
                    // and we got an id
                    .and().body("$", hasKey(EventConfig.ID))
                    .extract().body().jsonPath().prettify();
            // The response will send us back the whole book details so we update our test data
            payload.mergeIn(new JsonObject(responseBody));
        }
    }

    @Test
    @Order(5)
    void testQueryBooks() {
        LOGGER.info("\nTEST QUERY BOOKS ====================================================");
        // We check that the books were added to the the DB
        RestAssured.get("/data/books")
                .then().assertThat()
                .statusCode(HttpResponseStatus.OK.code())
                .and().body("$", hasSize(books.size()));

        LOGGER.info("Book data:");
        books.stream().map(JsonObject::encodePrettily).forEach(LOGGER::info);
    }

    @Test
    @Order(6)
    void testQueryAuthors() {
        LOGGER.info("\nTEST MATCH BOOK AUTHOR DATA =========================================");
        // We check that the authors were added to the the DB
        String responseBody = RestAssured.get("/data/authors")
                .then().assertThat()
                .statusCode(HttpResponseStatus.OK.code())
                .and().body("$", hasSize(authors.size()))
                .extract().body().jsonPath().prettify();
        LOGGER.info("Updating authors retrieved into test data");
        // At this point the response should include the book information on each author as well
        JsonArray authorsResponse = new JsonArray(responseBody);
        // iterate over each element in the returned array
        Stream.iterate(0, n -> n + 1)
                .limit(authorsResponse.size())
                .map(authorsResponse::getJsonObject)
                // and we match the author in the response to the one we have in test data
                .forEach(AppTest::matchWithBook);
    }

    @Test
    void testEndpointNotFound() {
        LOGGER.info("\nTEST ENDPOINT NOT FOUND =============================================");
        RestAssured.given()
                .accept("text/xml")
                .get("/data/authors")
                .then().assertThat()
                .statusCode(HttpResponseStatus.NOT_FOUND.code());

        // Vert.x responds with not found when the endpoint is correct but the media type does not match
        RestAssured.given()
                .contentType("text/xml")
                .accept(MediaTypes.APPLICATION_JSON)
                .body("Nonsensicalauthordata")
                .post("/data/author")
                .then().assertThat()
                .statusCode(HttpResponseStatus.NOT_FOUND.code());
    }

    @Test
    void testBadAuthorData() {
        LOGGER.info("\nTEST BAD AUTHOR DATA ================================================");
        JsonObject payload = new JsonObject().put("irrelevantKey", "irrelevantValue");
        request()
                .body(payload.encode())
                .post("/data/author")
                .then()
                .statusCode(HttpResponseStatus.BAD_REQUEST.code())
                .and().body("exception", hasToString(IllegalArgumentException.class.getName()));
    }

    @Test
    void testBadBookData() {
        LOGGER.info("\nTEST BAD BOOK DATA ==================================================");
        JsonObject payload = new JsonObject().put("irrelevantKey", "irrelevantValue");
        request()
                .body(payload.encode())
                .post("/data/book")
                .then()
                .statusCode(HttpResponseStatus.BAD_REQUEST.code())
                .and().body("exception", hasToString(IllegalArgumentException.class.getName()));
    }

    @Test
    void testBookWithUnknownAuthor() {
        LOGGER.info("\nTEST BOOK WITH UNKNOWN AUTHOR =======================================");
        JsonObject payload = new JsonObject()
                .put(EventConfig.TITLE, "The Unknown")
                .put(EventConfig.FORMAT, BookFormat.PAPERBACK.name())
                .put(EventConfig.AUTHORS, new JsonArray().add(666));
        request()
                .body(payload.encode())
                .post("/data/book")
                .then()
                .statusCode(HttpResponseStatus.NOT_FOUND.code())
                .and().body("exception", hasToString(NoResultException.class.getName()));
    }

    @Test
    void testBookWithOneMissingAuthor() {
        LOGGER.info("\nTEST BOOK WITH ONE MISSING AUTHOR ===================================");
        JsonObject payload = new JsonObject()
                .put(EventConfig.TITLE, "The Unknown")
                .put(EventConfig.FORMAT, BookFormat.PAPERBACK.name())
                .put(EventConfig.AUTHORS, new JsonArray().add(1).add(666));
        request()
                .body(payload.encode())
                .post("/data/book")
                .then()
                .statusCode(HttpResponseStatus.NOT_FOUND.code())
                .and().body("exception", hasToString(PartialResultException.class.getName()));
    }

    @Test
    void testDuplicateAuthors() {
        LOGGER.info("\nTEST DUPLICATE AUTHOR NAMES =========================================");
        Stream.of("Blake Victoria", "blake victoria", "BLAKE VICTORIA")
                .map(duplicatedName -> new JsonObject().put(EventConfig.NAME, duplicatedName))
                .forEach(payload -> request()
                        .body(payload.encode())
                        .post("/data/author")
                        .then().assertThat()
                        .statusCode(HttpResponseStatus.CONFLICT.code())
                        .and().body("exception", hasToString(PersistenceException.class.getName()))
                );
    }

    private static RequestSpecification request() {
        return RestAssured.given()
                .contentType(MediaTypes.APPLICATION_JSON)
                .accept(MediaTypes.APPLICATION_JSON);
    }

    private static Optional<JsonObject> findById(List<JsonObject> from, int id) {
        return from.stream()
                .filter(obj -> obj.getInteger(EventConfig.ID) == id)
                .findFirst();
    }

    private static void matchWithBook(JsonObject author) {
        LOGGER.debug("Matching author:\n{}", author.encodePrettily());

        Assertions.assertTrue(author.containsKey(EventConfig.BOOKS));

        // from the DB
        int authorId = author.getInteger(EventConfig.ID);

        // from test data
        boolean expectsBooks = authors.stream()
                .filter(authorData -> authorData.getId() == authorId)
                .findFirst()
                .map(authorData -> authorData.expectsBooks)
                .orElseThrow(() -> new IllegalArgumentException("Unknown author with id " + authorId));

        // If per test data we don't expect books on this author
        if (!expectsBooks) {
            // we check that effectively we didn't get books
            Assertions.assertTrue(author.getJsonArray(EventConfig.BOOKS).isEmpty());
            return;
        }

        Assertions.assertFalse(author.getJsonArray(EventConfig.BOOKS).isEmpty());

        JsonArray authorBooks = author.getJsonArray(EventConfig.BOOKS);

        // Let's grab each book id in the author.books[] array
        Disposable toDispose = Observable.range(0, authorBooks.size())
                .map(authorBooks::getInteger)
                // we find the book based on its id
                .map(bookId -> findById(books, bookId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No book found with id " + bookId + " for author with id: " + authorId
                        ))
                )
                // extract the authors
                .map(book -> book.getJsonArray(EventConfig.AUTHORS))
                // check if the book contains the author
                .filter(authorsInBook -> authorsInBook.contains(authorId))
                .count()
                .subscribe(
                        validBooks -> Assertions.assertEquals((long) validBooks, authorBooks.size()),
                        error -> {
                            LOGGER.error("Error matching author books", error);
                            LOGGER.error(author.encodePrettily());
                            books.stream().map(JsonObject::encode).forEach(LOGGER::error);
                            Assertions.fail();
                        }
                );
        toDispose.dispose();
    }

    private static String loadStringResource(String fileName) throws IOException {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        try (InputStream is = loader.getResourceAsStream(fileName)) {
            if (is == null) {
                throw new FileNotFoundException("File " + fileName + " yielded null input stream");
            }
            try (InputStreamReader isr = new InputStreamReader(is)) {
                BufferedReader reader = new BufferedReader(isr);
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
