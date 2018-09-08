package app.vyh.services.service;

import app.vyh.services.data.ChangeId;
import app.vyh.services.data.entity.AuthorEntity;
import app.vyh.services.data.entity.BookEntity;
import app.vyh.services.data.repository.AuthorsRepository;
import app.vyh.services.data.repository.BooksRepository;
import app.vyh.services.exception.LibraryOperationException;
import app.vyh.services.model.request.book.*;
import app.vyh.services.model.response.AuthorResponse;
import app.vyh.services.model.response.ChangeTypeEnum;
import app.vyh.services.model.response.LibraryChange;
import app.vyh.services.model.response.LibraryResponse;
import app.vyh.services.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.*;
import java.util.function.Function;

/**
 * @author Vic on 9/1/2018
 **/
@Component
@RequestScope
public class LibraryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryService.class);
    private static final String BOOK_FIELD = "book";
    private static final String BOOKS_FIELD = BOOK_FIELD + "s";
    private static final String AUTHOR_FIELD = "author";
    private static final String AUTHORS_FIELD = AUTHOR_FIELD + "s";

    private HttpStatus finalStatus = HttpStatus.OK;
    private HttpHeaders httpHeaders = new HttpHeaders();
    private LibraryResponse response = new LibraryResponse();

    private final BooksRepository booksRepository;
    private final AuthorsRepository authorsRepository;
    private final ChangeId changeId;

    @Autowired
    public LibraryService(BooksRepository booksRepository, AuthorsRepository authorsRepository, ChangeId changeId) {
        this.booksRepository = booksRepository;
        this.authorsRepository = authorsRepository;
        this.changeId = changeId;
    }

    /*
    BOOK COLLECTION
     */
    public LibraryResponse getBooks() throws LibraryOperationException {

        return getAll(booksRepository, BookEntity::getId, BookEntity::toResponse, BOOKS_FIELD);
    }

    public LibraryResponse addBook(AddBookRequest request) throws LibraryOperationException {
        LOGGER.debug("Adding new book");
        LOGGER.trace(request.toString());

        if (request.getAuthors().isEmpty()) {
            throw new LibraryOperationException("Author(s) MUST be defined", HttpStatus.BAD_REQUEST);
        }

        BookEntity newBook = new BookEntity(request);
        LOGGER.debug("BookEntity created with id: " + newBook.getId());

        Optional<BookEntity> existentBook = booksRepository.findById(newBook.getId());

        LOGGER.trace("Existent Book? " + existentBook.isPresent());
        if (existentBook.isPresent()) {
            throw new LibraryOperationException("Book already exists", HttpStatus.CONFLICT);
        }

        LibraryChange responseContent = new LibraryChange();
        processBookAuthors(newBook, responseContent, request.getAuthors());
        LOGGER.trace(newBook.toString());

        LOGGER.debug("Saving book");
        booksRepository.save(newBook);
        LOGGER.debug("Book saved");


        finalStatus = HttpStatus.CREATED;
        httpHeaders.add(HttpHeaders.LOCATION, "/books/" + newBook.getId());

        LOGGER.debug("Building response");
        responseContent.getBooks().add(newBook.toResponse().withAction(ChangeTypeEnum.ADDED));

        return responseWithChange(responseContent);
    }

    /*
    BOOK RESOURCE
     */
    public LibraryResponse getBook(Integer bookId) throws LibraryOperationException {
        return getById(bookId, booksRepository, BookEntity::toResponse, BOOK_FIELD);
    }

    public LibraryResponse updateBook(Integer bookId, AddBookRequest request) throws LibraryOperationException {
        LOGGER.debug("Updating book with id: " + bookId);
        LOGGER.trace(request.toString());

        BookEntity newBook = new BookEntity(request);
        LOGGER.debug("Book replacement created with id: " + newBook.getId());
        LOGGER.trace(newBook.toString());

        Optional<BookEntity> possibleConflict = booksRepository.findById(newBook.getId());
        if (possibleConflict.isPresent()) {
            throw new LibraryOperationException("There is already a book with the updated information with book id: " + newBook.getId(), HttpStatus.CONFLICT);
        }

        if (request.getAuthors().isEmpty()) {
            throw new LibraryOperationException("Author(s) MUST be defined in updated data", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);

        LOGGER.debug("Removing author information from old book");
        existentBook.clearAuthors();
        LOGGER.debug("Authors cleared");

        LOGGER.debug("Deleting old book info");
        booksRepository.delete(existentBook);
        LOGGER.debug("Book deleted");

        LibraryChange responseContent = new LibraryChange();
        newBook.setQuantity(existentBook.getQuantity());

        processBookAuthors(newBook, responseContent, request.getAuthors());
        LOGGER.trace(newBook.toString());

        LOGGER.debug("Saving new book info");
        booksRepository.save(newBook);
        LOGGER.debug("Book saved");

        responseContent.withBooks(
                newBook.toResponse().withAction(ChangeTypeEnum.ADDED),
                existentBook.toResponse().withAction(ChangeTypeEnum.DELETED)
        );

        return responseWithChange(responseContent);
    }

    public LibraryResponse updateBookQty(Integer bookId, UpdateBookRequest request) throws LibraryOperationException {
        LOGGER.debug("Updating book with id: " + bookId);
        LOGGER.trace(request.toString());

        if (request.getQuantity() == 0) {
            throw new LibraryOperationException("Quantity to update must be > 0", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);

        byte currentQty = existentBook.getQuantity();
        byte delta = new Long(request.getQuantity()).byteValue();
        LOGGER.debug("Current qty: " + currentQty + " will " + request.getAction() + " by " + delta);

        switch (request.getAction()) {
            case INCREASE:
                currentQty += delta;
                break;
            case DECREASE:
                currentQty -= delta;
                if (currentQty < 0) {
                    currentQty = 0;
                }
                break;
            default:
                throw new LibraryOperationException(
                        "Action to perform not recognized: " + request.getAction() + ". Possible values are " + Arrays.toString(UpdateQtyActionEnum.values()),
                        HttpStatus.NOT_ACCEPTABLE
                );
        }

        existentBook.setQuantity(currentQty);

        LOGGER.debug("Saving updated book info");
        booksRepository.save(existentBook);
        LOGGER.debug("Book saved");

        LibraryChange responseContent = new LibraryChange();
        responseContent.getBooks().add(existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED));

        return responseWithChange(responseContent);
    }

    public LibraryResponse deleteBook(Integer bookId) throws LibraryOperationException {
        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);

        LOGGER.debug("Getting authors from existent book");
        Set<Integer> bookAuthorIds = new HashSet<>();
        existentBook.getAuthors().forEach(authorEntity -> bookAuthorIds.add(authorEntity.getId()));

        LOGGER.debug("Removing author information from existent book");
        existentBook.clearAuthors();
        LOGGER.debug("Authors cleared");

        LOGGER.debug("Deleting existent book info");
        booksRepository.delete(existentBook);
        LOGGER.debug("Book deleted");

        LibraryChange responseContent = new LibraryChange();
        AuthorEntity authorToUpdate;
        LOGGER.debug("Retrieving updated author information");
        for (Integer authorId : bookAuthorIds) {
            LOGGER.debug("Getting author with id: " + authorId);
            try {
                authorToUpdate = findById(authorId, authorsRepository, AUTHOR_FIELD);
                if (authorToUpdate.getBooks().isEmpty()) {
                    LOGGER.debug("No more books defined for author. Removing author");
                    authorsRepository.delete(authorToUpdate);
                    LOGGER.debug("Author deleted");
                }
                responseContent.getAuthors().add(
                        authorToUpdate.toResponse().withAction(
                                authorToUpdate.getBooks().isEmpty()
                                        ? ChangeTypeEnum.DELETED
                                        : ChangeTypeEnum.UPDATED
                        )
                );
            } catch (LibraryOperationException exception) {
                LOGGER.error("Author not found for some reason, we're send it in response as error", exception);
                finalStatus = HttpStatus.PARTIAL_CONTENT;
                responseContent.getAuthors().add(
                        new AuthorResponse()
                                .withId(authorId)
                                .withAction(ChangeTypeEnum.ERROR)
                                .withErrorDescription(exception.getMessage())
                );
            }
        }

        responseContent.getBooks().add(existentBook.toResponse().withAction(ChangeTypeEnum.DELETED));

        return responseWithChange(responseContent);
    }

    /*
    AUTHORS BY BOOK COLLECTION
     */
    public LibraryResponse getAuthorsByBook(Integer bookId) throws LibraryOperationException {
        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);

        LOGGER.debug("Got " + existentBook.getAuthors().size() + " authors in book");
        Map<Integer, AuthorResponse> entitiesInResponse = new HashMap<>();
        existentBook.getAuthors().forEach(author -> entitiesInResponse.put(author.getId(), author.toResponse()));
        LOGGER.debug("Authors mapped");

        return response.withHttpStatusCode(finalStatus.value())
                .withContent(entitiesInResponse)
                .withChangeId(changeId.getChangeId());
    }

    public LibraryResponse addAuthorsToBook(Integer bookId, AddAuthorRequest request) throws LibraryOperationException {
        if (request.getId().isEmpty() && request.getName().isEmpty()) {
            throw new LibraryOperationException("Authors to add MUST be defined", HttpStatus.BAD_REQUEST);
        }
        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);

        LibraryChange responseContent = new LibraryChange();

        if (!request.getId().isEmpty()) {
            LOGGER.debug("Updating author information by id into book");
            AuthorEntity author;
            for (Integer authorId : request.getId()) {
                try {
                    author = findById(authorId, authorsRepository, AUTHORS_FIELD);
                    existentBook.addAuthor(author);
                    responseContent.getAuthors().add(
                            author.toResponse().withAction(ChangeTypeEnum.UPDATED)
                    );
                } catch (LibraryOperationException exception) {
                    LOGGER.error("Author with id " + authorId + " not found", exception);
                    finalStatus = HttpStatus.PARTIAL_CONTENT;
                    responseContent.getAuthors().add(
                            new AuthorResponse().withId(authorId)
                                    .withAction(ChangeTypeEnum.ERROR)
                                    .withErrorDescription(exception.getMessage())
                    );
                }
            }
        }

        if (!request.getName().isEmpty()) {
            processBookAuthors(existentBook, responseContent, request.getName());
        }

        LOGGER.debug("Saving updated book info");
        booksRepository.save(existentBook);
        LOGGER.debug("Book saved");

        responseContent.getBooks().add(existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED));

        return responseWithChange(responseContent);
    }

    /*
    AUTHORS BY BOOK RESOURCE
     */
    public LibraryResponse deleteAuthorFromBook(Integer bookId, Integer authorId) throws LibraryOperationException {
        BookEntity existentBook = findById(bookId, booksRepository, BOOK_FIELD);
        AuthorEntity authorInBook = null;

        for (AuthorEntity authorEntity : existentBook.getAuthors()) {
            if (authorId.equals(authorEntity.getId())) {
                authorInBook = authorEntity;
                break;
            }
        }

        if (authorInBook == null) {
            throw new LibraryOperationException("No author with id " + authorId + " was found in book with id " + bookId, HttpStatus.NOT_FOUND);
        }

        if (existentBook.getAuthors().size() == 1) {
            throw new LibraryOperationException("Author with id " + authorId + " matches the only author defined for the book. Books MUST have at least one author", HttpStatus.FORBIDDEN);
        }

        LOGGER.debug("Removing author from book");
        existentBook.removeAuthor(authorInBook);
        LOGGER.debug("Author removed");
        LOGGER.trace("Book updated: " + existentBook.toString());
        LOGGER.trace("Author updated: " + authorInBook.toString());

        booksRepository.save(existentBook);

        LibraryChange responseContent = new LibraryChange()
                .withBooks(existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED))
                .withAuthors(authorInBook.toResponse().withAction(ChangeTypeEnum.UPDATED));

        return responseWithChange(responseContent);
    }

    /*
    AUTHOR COLLECTION
     */

    public LibraryResponse getAuthors() throws LibraryOperationException {
        return getAll(authorsRepository, AuthorEntity::getId, AuthorEntity::toResponse, AUTHORS_FIELD);
    }

    /*
    AUTHOR RESOURCE
     */

    public LibraryResponse getAuthor(Integer authorId) throws LibraryOperationException {
        return getById(authorId, authorsRepository, AuthorEntity::toResponse, AUTHOR_FIELD);
    }

    public LibraryResponse updateAuthor(Integer authorId, UpdateAuthorRequest request) throws LibraryOperationException {
        LOGGER.debug("Updating author with id: " + authorId);
        LOGGER.trace(request.toString());

        AuthorEntity newAuthor = new AuthorEntity(request);
        LOGGER.debug("Author replacement created with id: " + newAuthor.getId());
        LOGGER.trace(newAuthor.toString());

        Optional<AuthorEntity> possibleConflict = authorsRepository.findById(newAuthor.getId());
        if (possibleConflict.isPresent()) {
            throw new LibraryOperationException("There is already an author with the updated information with id: " + newAuthor.getId(), HttpStatus.CONFLICT);
        }

        if (StringUtils.notValidString(request.getName())) {
            throw new LibraryOperationException("Author name MUST be defined for update", HttpStatus.BAD_REQUEST);
        }

        AuthorEntity existentAuthor = findById(authorId, authorsRepository, AUTHOR_FIELD);
        LibraryChange responseContent = new LibraryChange();

        LOGGER.debug("Adding new author to the books");
        existentAuthor.getBooks().forEach(bookEntity -> bookEntity.addAuthor(newAuthor));
        LOGGER.trace(newAuthor.toString());

        LOGGER.debug("Removing old author from the books");
        newAuthor.getBooks().forEach(bookEntity -> {
            bookEntity.removeAuthor(existentAuthor);
            responseContent.getBooks().add(bookEntity.toResponse().withAction(ChangeTypeEnum.UPDATED));
        });
        LOGGER.trace(existentAuthor.toString());

        LOGGER.debug("Deleting old author");
        authorsRepository.delete(existentAuthor);

        LOGGER.debug("Saving new author");
        authorsRepository.save(newAuthor);

        responseContent.withAuthors(
                newAuthor.toResponse().withAction(ChangeTypeEnum.ADDED),
                existentAuthor.toResponse().withAction(ChangeTypeEnum.DELETED)
        );

        return responseWithChange(responseContent);
    }

    private <E, R> LibraryResponse getAll(JpaRepository<E, Integer> repository, Function<E, Integer> toId, Function<E, R> toResponse, String field)
    throws LibraryOperationException {
        LOGGER.debug("Getting all " + field);

        List<E> allEntities = repository.findAll();

        if (allEntities == null) {
            throw new LibraryOperationException("Something went wrong with retrieving all " + field, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (allEntities.isEmpty()) {
            finalStatus = HttpStatus.NO_CONTENT;
        } else {
            LOGGER.debug("Got all " + field + ": " + allEntities.size() + " in library");
            Map<Integer, R> entitiesInResponse = new HashMap<>();
            allEntities.forEach(entity -> entitiesInResponse.put(toId.apply(entity), toResponse.apply(entity)));
            LOGGER.debug("Entities mapped");
            response.setContent(entitiesInResponse);
        }

        return response.withHttpStatusCode(finalStatus.value())
                .withChangeId(changeId.getChangeId());
    }

    private <E, R> LibraryResponse getById(Integer entityId, JpaRepository<E, Integer> repository, Function<E, R> toResponse, String field)
    throws LibraryOperationException {
        E found = findById(entityId, repository, field);

        Map<Integer, R> entitiesInResponse = new HashMap<>(1);
        entitiesInResponse.put(entityId, toResponse.apply(found));
        LOGGER.trace(entitiesInResponse.toString());

        return response.withHttpStatusCode(finalStatus.value())
                .withContent(entitiesInResponse)
                .withChangeId(changeId.getChangeId());
    }

    private <E> E findById(Integer entityId, JpaRepository<E, Integer> repository, String field)
    throws LibraryOperationException {
        LOGGER.debug("Searching " + field + " with id: " + entityId);
        E result = repository.findById(entityId).orElseThrow(() -> new LibraryOperationException(
                field + " with id " + entityId + " not found",
                HttpStatus.NOT_FOUND
        ));
        LOGGER.trace(result.toString());
        return result;
    }

    private void processBookAuthors(BookEntity book, LibraryChange responseContent, final List<String> authors) {
        AuthorEntity author;
        Optional<AuthorEntity> existentAuthor;
        LOGGER.debug("Updating author information by name into book");

        for (String authorName : authors) {
            LOGGER.debug("Searching for author: " + authorName);

            author = new AuthorEntity(authorName);
            LOGGER.trace("Author entity: " + author.toString());

            // Here we search for the author in the DB
            existentAuthor = authorsRepository.findById(author.getId());

            LOGGER.trace("Existent author? " + existentAuthor.isPresent());
            if (existentAuthor.isPresent()) {
                // If it's there, we use that one for the book
                author = existentAuthor.get();
                LOGGER.debug("Author existed in the db");
            } else {
                // If not, we will just use the new one, but we save it to the db first
                authorsRepository.save(author);
                LOGGER.debug("Author saved to the db");
            }
            LOGGER.trace(author.toString());

            // We add the author to the book
            book.addAuthor(author);
            responseContent.getAuthors().add(
                    author.toResponse().withAction(
                            existentAuthor.isPresent() ? ChangeTypeEnum.UPDATED : ChangeTypeEnum.ADDED
                    )
            );
            LOGGER.debug("Author added to the book, with id: " + author.getId());
        }
        LOGGER.debug("Author information updated");
    }

    private LibraryResponse responseWithChange(LibraryChange responseContent) {
        ChangeId.ChangeUpdate changeUpdate = changeId.update();
        return response.withHttpStatusCode(finalStatus.value())
                .withContent(responseContent)
                .withChangeId(changeUpdate.getAfter())
                .withLastChangeId(changeUpdate.getBefore());
    }

    public HttpStatus getFinalStatus() {
        return finalStatus;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }
}
