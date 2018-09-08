package app.vyh.services;

import app.vyh.services.data.entity.BookFormat;
import app.vyh.services.model.request.book.AddBookRequest;
import app.vyh.services.model.response.LibraryChange;
import app.vyh.services.model.response.LibraryResponse;
import app.vyh.services.util.TestUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Unit test for simple App.
 *
 * @author Vic on 8/28/2018
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppTest.class);
    private static final String HOST = "http://localhost:";

    @LocalServerPort
    private int port;

    public void validatePostBook(AddBookRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<AddBookRequest> postEntity = new HttpEntity<>(request, headers);
        HttpEntity<AddBookRequest> getEntity = new HttpEntity<>(headers);

        ResponseEntity<LibraryResponse> responseEntity;

        try {
            responseEntity = restTemplate.exchange(
                    HOST + port + "/books",
                    HttpMethod.POST,
                    postEntity,
                    LibraryResponse.class
            );
            Assert.assertNotNull(responseEntity);
            Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.CREATED);

            Assert.assertNotNull(responseEntity.getBody());

            // Validate the response
            LibraryResponse response = responseEntity.getBody();
            Assert.assertEquals(HttpStatus.CREATED.value(), response.getHttpStatusCode());
            Assert.assertNull(response.getError());
            Assert.assertNotNull(response.getContent());
//            System.err.println(response.getContent().getClass());
//            Assert.assertTrue(response.getContent() instanceof LibraryChange);
//
//            // Validate response content
//            LibraryChange responseContent = (LibraryChange) response.getContent();
//            TestUtils.assertList(responseContent.getBooks(), 1);
//            TestUtils.assertList(responseContent.getBooks(), request.getAuthors().size());

            // Validate the headers
            Assert.assertNotNull(responseEntity.getHeaders());
            Assert.assertNotNull(responseEntity.getHeaders().get(HttpHeaders.LOCATION));
            Assert.assertFalse(Objects.requireNonNull(responseEntity.getHeaders().get(HttpHeaders.LOCATION)).isEmpty());

            String followUpPath = Objects.requireNonNull(responseEntity.getHeaders().get(HttpHeaders.LOCATION)).get(0);
            Assert.assertNotNull(followUpPath);
            Assert.assertFalse(followUpPath.isEmpty());

            responseEntity = restTemplate.exchange(
                    HOST + port + followUpPath,
                    HttpMethod.GET,
                    getEntity,
                    LibraryResponse.class
            );

            Assert.assertNotNull(responseEntity);
            Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
            Assert.assertNotNull(responseEntity.getBody());

            response = responseEntity.getBody();
            Assert.assertEquals(HttpStatus.OK.value(), response.getHttpStatusCode());
            Assert.assertNull(response.getError());
            Assert.assertNotNull(response.getContent());
            Assert.assertTrue(response.getContent() instanceof Map);

        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Something went wrong; response: " + exception.getResponseBodyAsString());
            Assert.fail();
        }
    }

    @Test
    public void a_testPostingBooks() {
        AddBookRequest request = (AddBookRequest) new AddBookRequest()
                .withAuthors("TONY TIGER", "SAM TOUCAN", "FRANK PANTHER")
                .withTitle("FIRST BOOK TO TEST")
                .withIsbn("1234567890")
                .withYear("2018")
                .withFormat(BookFormat.HC);

        validatePostBook(request);
    }

    @Test
    public void b_testGettingBooks() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<AddBookRequest> getEntity = new HttpEntity<>(headers);
        ResponseEntity<LibraryResponse> responseEntity;
        try {
            responseEntity = restTemplate.exchange(
                    HOST + port + "/books",
                    HttpMethod.GET,
                    getEntity,
                    LibraryResponse.class
            );
            Assert.assertNotNull(responseEntity);
            Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Something went wrong; response: " + exception.getResponseBodyAsString());
            Assert.fail();
        }
    }
}
