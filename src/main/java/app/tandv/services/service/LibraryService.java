package app.tandv.services.service;

import app.tandv.services.data.ChangeId;
import app.tandv.services.data.entity.BookEntity;
import app.tandv.services.data.repository.LibraryRepository;
import app.tandv.services.exception.LibraryOperationException;
import app.tandv.services.util.StringUtils;
import app.tandv.services.data.entity.AuthorEntity;
import app.tandv.services.model.request.book.*;
import app.tandv.services.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vic on 9/1/2018
 **/
@Component
@RequestScope
public class LibraryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryService.class);

    private HttpStatus finalStatus = HttpStatus.OK;
    private HttpHeaders httpHeaders = new HttpHeaders();
    private LibraryResponse response = new LibraryResponse();

    private final ChangeId changeId;
    private final LibraryRepository repository;

    @Qualifier("build.properties")
    private final Map<String, String> buildProperties;

    @Autowired
    public LibraryService(ChangeId changeId, LibraryRepository repository, Map<String, String> buildProperties) {
        this.changeId = changeId;
        this.repository = repository;
        this.buildProperties = buildProperties;
    }

    /*
    BOOK COLLECTION
     */
    public LibraryResponse getBooks() throws LibraryOperationException {
        Optional<Map<Integer, Object>> allEntities = repository.getAll(BookEntity.class);

        if (allEntities.isPresent()) {
            response.setContent(allEntities.get());
        } else {
            response.setContent(Collections.EMPTY_MAP);
        }

        return response.withHttpStatusCode(finalStatus.value())
                .withChangeId(changeId.getChangeId());
    }

    public LibraryResponse addBook(AddBookRequest request)
    throws LibraryOperationException {
        if (request.getAuthors().isEmpty()) {
            throw new LibraryOperationException("Author(s) MUST be defined", HttpStatus.BAD_REQUEST);
        }

        // TODO: change this for a method that I can tap into for advice
        BookEntity newBook = new BookEntity(request);

        Optional<BookEntity> existentBook = repository.findById(BookEntity.class, newBook.getId());

        if (existentBook.isPresent()) {
            throw new LibraryOperationException("Book already exists", HttpStatus.CONFLICT);
        }

        LibraryChange responseContent = new LibraryChange();
        addAuthorsByName(newBook, responseContent, request.getAuthors());
        LOGGER.trace(newBook.toString());

        repository.save(BookEntity.class, newBook);

        finalStatus = HttpStatus.CREATED;
        httpHeaders.add(HttpHeaders.LOCATION, "/books/" + newBook.getId());

        return responseWithChange(
                responseContent.withBooks(
                        newBook.toResponse().withAction(ChangeTypeEnum.ADDED)
                )
        );
    }

    /*
    BOOK RESOURCE
     */
    public LibraryResponse getBook(Integer bookId)
    throws LibraryOperationException {
        BookEntity found = repository.fetchById(BookEntity.class, bookId);

        Map<Integer, Object> entitiesInResponse = new HashMap<>(1);
        entitiesInResponse.put(bookId, found.toResponse());

        return responseWithNoChange(entitiesInResponse);
    }

    public LibraryResponse updateBook(Integer bookId, AddBookRequest request)
    throws LibraryOperationException {
        BookEntity newBook = new BookEntity(request);

        Optional<BookEntity> possibleConflict = repository.findById(BookEntity.class, newBook.getId());
        if (possibleConflict.isPresent()) {
            throw new LibraryOperationException(
                    "There is already a book with the updated information with book id: " + newBook.getId(),
                    HttpStatus.CONFLICT
            );
        }

        if (request.getAuthors().isEmpty()) {
            throw new LibraryOperationException("Author(s) MUST be defined in updated data", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);

        LibraryChange responseContent = new LibraryChange();
        Set<Integer> bookAuthorIds = clearAuthorsFromBook(existentBook, responseContent, true);

        newBook.setQuantity(existentBook.getQuantity());
        addAuthorsByName(newBook, responseContent, request.getAuthors());
        LOGGER.trace(newBook.toString());

        repository.save(BookEntity.class, newBook);

        safeDeleteAuthors(bookAuthorIds, responseContent);

        responseContent.getBooks().add(
                newBook.toResponse().withAction(ChangeTypeEnum.ADDED)
        );
        return responseWithChange(responseContent);
    }

    public LibraryResponse updateBookQuantity(Integer bookId, UpdateBookQuantityRequest request)
    throws LibraryOperationException {
        if (request.getQuantity() == 0) {
            throw new LibraryOperationException("Quantity to update must be > 0", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);

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
                        "Action to perform not recognized: " + request.getAction()
                                + ". Possible values are " + Arrays.toString(UpdateQtyActionEnum.values()),
                        HttpStatus.NOT_ACCEPTABLE
                );
        }

        existentBook.setQuantity(currentQty);
        repository.save(BookEntity.class, existentBook);
        return responseWithChange(
                new LibraryChange().withBooks(
                        existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED)
                )
        );
    }

    public LibraryResponse updateBookTitle(Integer bookId, UpdateBookTitleRequest request)
    throws LibraryOperationException {
        if (StringUtils.notValidString(request.getTitle())) {
            throw new LibraryOperationException("Invalid name for book update", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);

        LibraryChange responseContent = new LibraryChange();
        String newOrderingTitle = StringUtils.titleForOrdering(request.getTitle());

        if (newOrderingTitle.equals(existentBook.getOrderingTitle())) {
            LOGGER.debug("Ordering title matches new title. Updating existent book");
            existentBook.setTitle(request.getTitle());
            repository.save(BookEntity.class, existentBook);
            responseContent.getBooks().add(existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED));
        } else {
            LOGGER.debug("Ordering title mismatch from new title. Full book updated needed.");
            // TODO: add this to the producer method
            BookEntity newBook = new BookEntity(request.getTitle(), existentBook.getIsbn(), existentBook.getYear(), existentBook.getFormat());
            if (!newBook.getTitle().equals(request.getTitle())) {
                // Here we are setting the requested title in the case was given
                newBook.setTitle(request.getTitle());
            }

            Optional<BookEntity> possibleConflict = repository.findById(BookEntity.class, newBook.getId());
            if (possibleConflict.isPresent()) {
                throw new LibraryOperationException("There is already a book with the updated title with book id: " + newBook.getId(), HttpStatus.CONFLICT);
            }
            LOGGER.debug("Adding author information to the new book");
            existentBook.getAuthors().forEach(newBook::addAuthor);
            existentBook.clearAuthors();
            repository.delete(BookEntity.class, existentBook);
            repository.save(BookEntity.class, newBook);
            responseContent
                    .withBooks(
                            newBook.toResponse().withAction(ChangeTypeEnum.ADDED),
                            existentBook.toResponse().withAction(ChangeTypeEnum.DELETED)
                    )
                    .withAuthors(
                            newBook.getAuthors()
                                    .stream()
                                    .map(authorEntity -> authorEntity.toResponse().withAction(ChangeTypeEnum.UPDATED))
                                    .collect(Collectors.toSet())
                    );
        }

        return responseWithChange(responseContent);
    }

    public LibraryResponse deleteBook(Integer bookId)
    throws LibraryOperationException {
        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);
        LibraryChange responseContent = new LibraryChange();
        Set<Integer> bookAuthorIds = clearAuthorsFromBook(existentBook, responseContent, true);
        safeDeleteAuthors(bookAuthorIds, responseContent);
        return responseWithChange(responseContent);
    }

    /*
    AUTHORS BY BOOK RESOURCE
     */
    public LibraryResponse getAuthorsByBook(Integer bookId)
    throws LibraryOperationException {
        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);

        LOGGER.debug("Got " + existentBook.getAuthors().size() + " authors in book");
        Map<Integer, AuthorResponse> entitiesInResponse = new HashMap<>();
        existentBook.getAuthors().forEach(author -> entitiesInResponse.put(author.getId(), author.toResponse()));
        LOGGER.debug("Authors mapped");

        return response.withHttpStatusCode(finalStatus.value())
                .withContent(entitiesInResponse)
                .withChangeId(changeId.getChangeId());
    }

    public LibraryResponse addAuthorsToBook(Integer bookId, AddAuthorRequest request)
    throws LibraryOperationException {
        return addAuthors(bookId, request, false);
    }

    public LibraryResponse updateBookAuthors(Integer bookId, AddAuthorRequest request)
    throws LibraryOperationException {
        return addAuthors(bookId, request, true);
    }

    /*
    AUTHORS BY BOOK RESOURCE
     */
    public LibraryResponse deleteAuthorFromBook(Integer bookId, Integer authorId)
    throws LibraryOperationException {
        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);
        AuthorEntity authorInBook = null;

        for (AuthorEntity authorEntity : existentBook.getAuthors()) {
            if (authorId.equals(authorEntity.getId())) {
                authorInBook = authorEntity;
                break;
            }
        }

        if (authorInBook == null) {
            throw new LibraryOperationException(
                    "No author with id " + authorId + " was found in book with id " + bookId,
                    HttpStatus.NOT_FOUND
            );
        }

        if (existentBook.getAuthors().size() == 1) {
            throw new LibraryOperationException(
                    "Author with id " + authorId
                            + " matches the only author defined for the book. Books MUST have at least one author",
                    HttpStatus.FORBIDDEN
            );
        }

        existentBook.removeAuthor(authorInBook);

        LOGGER.trace("Book updated: " + existentBook.toString());
        LOGGER.trace("Author updated: " + authorInBook.toString());

        repository.save(BookEntity.class, existentBook);

        if (authorInBook.getBooks().isEmpty()) {
            LOGGER.debug("Author does not have any more books. Removing");
            repository.delete(AuthorEntity.class, authorInBook);
        }

        LibraryChange responseContent = new LibraryChange()
                .withBooks(existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED))
                .withAuthors(
                        authorInBook.toResponse()
                                .withAction(
                                        authorInBook.getBooks().isEmpty()
                                                ? ChangeTypeEnum.DELETED
                                                : ChangeTypeEnum.UPDATED
                                )
                );
        return responseWithChange(responseContent);
    }

    /*
    AUTHOR COLLECTION
     */

    public LibraryResponse getAuthors()
    throws LibraryOperationException {
        Optional<Map<Integer, Object>> allEntities = repository.getAll(AuthorEntity.class);

        if (allEntities.isPresent()) {
            response.setContent(allEntities.get());
        } else {
            response.setContent(Collections.EMPTY_MAP);
        }

        return response.withHttpStatusCode(finalStatus.value())
                .withChangeId(changeId.getChangeId());
    }

    /*
    AUTHOR RESOURCE
     */

    public LibraryResponse getAuthor(Integer authorId)
    throws LibraryOperationException {
        AuthorEntity found = repository.fetchById(AuthorEntity.class, authorId);

        Map<Integer, Object> entitiesInResponse = new HashMap<>(1);
        entitiesInResponse.put(authorId, found.toResponse());

        return responseWithNoChange(entitiesInResponse);
    }

    public LibraryResponse updateAuthor(Integer authorId, UpdateAuthorRequest request)
    throws LibraryOperationException {
        // TODO: create producer method for this
        AuthorEntity newAuthor = new AuthorEntity(request);

        Optional<AuthorEntity> possibleConflict = repository.findById(AuthorEntity.class, newAuthor.getId());
        if (possibleConflict.isPresent()) {
            throw new LibraryOperationException(
                    "There is already an author with the updated information with id: " + newAuthor.getId(),
                    HttpStatus.CONFLICT
            );
        }

        if (StringUtils.notValidString(request.getName())) {
            throw new LibraryOperationException("Author name MUST be defined for update", HttpStatus.BAD_REQUEST);
        }

        AuthorEntity existentAuthor = repository.fetchById(AuthorEntity.class, authorId);
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

        repository.delete(AuthorEntity.class, existentAuthor);
        repository.save(AuthorEntity.class, newAuthor);

        responseContent.withAuthors(
                newAuthor.toResponse().withAction(ChangeTypeEnum.ADDED),
                existentAuthor.toResponse().withAction(ChangeTypeEnum.DELETED)
        );

        return responseWithChange(responseContent);
    }

    public LibraryResponse getServiceStatus() {
        return responseWithNoChange(buildProperties);
    }

    private LibraryResponse addAuthors(Integer bookId, final AddAuthorRequest request, boolean clearPreviousAuthors)
    throws LibraryOperationException {
        if (request.getId().isEmpty() && request.getName().isEmpty()) {
            throw new LibraryOperationException("Authors to add MUST be defined", HttpStatus.BAD_REQUEST);
        }

        BookEntity existentBook = repository.fetchById(BookEntity.class, bookId);
        LibraryChange responseContent = new LibraryChange();

        Set<Integer> bookAuthorIds = null;
        if (clearPreviousAuthors) {
            bookAuthorIds = clearAuthorsFromBook(existentBook, responseContent, false);
        }

        if (!request.getId().isEmpty()) {
            addAuthorsById(existentBook, responseContent, request.getId());
        }

        if (!request.getName().isEmpty()) {
            addAuthorsByName(existentBook, responseContent, request.getName());
        }

        if (clearPreviousAuthors) {
            safeDeleteAuthors(bookAuthorIds, responseContent);
        }

        repository.save(BookEntity.class, existentBook);

        return responseWithChange(
                responseContent.withBooks(
                        existentBook.toResponse().withAction(ChangeTypeEnum.UPDATED)
                )
        );
    }

    private void addAuthorsById(BookEntity book, LibraryChange responseContent, final List<Integer> authorIds) {
        Optional<AuthorEntity> possibleAuthor;
        AuthorEntity author;
        LOGGER.debug("Updating author information by id into book");
        for (Integer authorId : authorIds) {
            possibleAuthor = repository.findById(AuthorEntity.class, authorId);
            if (possibleAuthor.isPresent()) {
                author = possibleAuthor.get();
                book.addAuthor(author);
                responseContent.getAuthors().add(
                        author.toResponse().withAction(ChangeTypeEnum.UPDATED)
                );
            } else {
                LOGGER.error("Author with id " + authorId + " not found");
                finalStatus = HttpStatus.PARTIAL_CONTENT;
                responseContent.getAuthors().add(
                        new AuthorResponse().withId(authorId)
                                .withAction(ChangeTypeEnum.ERROR)
                                .withErrorDescription("Author with id " + authorId + " not found")
                );
            }
        }
    }

    private void addAuthorsByName(BookEntity book, LibraryChange responseContent, final List<String> authorNames) {
        AuthorEntity author;
        Optional<AuthorEntity> existentAuthor;
        LOGGER.debug("Updating author information by name into book");

        for (String authorName : authorNames) {
            LOGGER.debug("Searching for author: " + authorName);

            // TODO: add provider method for this too
            author = new AuthorEntity(authorName);
            existentAuthor = repository.findById(AuthorEntity.class, author.getId());
            if (existentAuthor.isPresent()) {
                author = existentAuthor.get();
                LOGGER.debug("Author existed in the db");
            } else {
                repository.save(AuthorEntity.class, author);
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

    private Set<Integer> clearAuthorsFromBook(BookEntity book, LibraryChange responseContent, boolean deleteBook) {
        LOGGER.debug("Getting authors from existent book");
        Set<Integer> bookAuthorIds = new HashSet<>();
        book.getAuthors().forEach(authorEntity -> bookAuthorIds.add(authorEntity.getId()));
        book.clearAuthors();
        if (deleteBook) {
            repository.delete(BookEntity.class, book);
            responseContent.getBooks().add(book.toResponse().withAction(ChangeTypeEnum.DELETED));
        }
        return bookAuthorIds;
    }

    private void safeDeleteAuthors(final Set<Integer> authorIds, LibraryChange responseContent) {
        Optional<AuthorEntity> possibleAuthor;
        AuthorEntity authorToUpdate;
        AuthorResponse responseToAdd;

        LOGGER.debug("Retrieving updated author information");
        for (Integer authorId : authorIds) {
            LOGGER.debug("Getting author with id: " + authorId);
            possibleAuthor = repository.findById(AuthorEntity.class, authorId);
            if (possibleAuthor.isPresent()) {
                authorToUpdate = possibleAuthor.get();
                if (authorToUpdate.getBooks().isEmpty()) {
                    repository.delete(AuthorEntity.class, authorToUpdate);
                }
                responseToAdd = authorToUpdate.toResponse()
                        .withAction(
                                authorToUpdate.getBooks().isEmpty()
                                        ? ChangeTypeEnum.DELETED
                                        : ChangeTypeEnum.UPDATED
                        );
            } else {
                LOGGER.error("Author not found for some reason, we're send it in response as error");
                finalStatus = HttpStatus.PARTIAL_CONTENT;
                responseToAdd = new AuthorResponse()
                        .withId(authorId)
                        .withAction(ChangeTypeEnum.ERROR)
                        .withErrorDescription("Author with id " + authorId + " not found despite being part of a book");
            }
            responseContent.getAuthors().add(responseToAdd);
        }
        LOGGER.debug("Orphan authors deleted");
    }

    private LibraryResponse responseWithChange(LibraryChange responseContent) {
        ChangeId.ChangeUpdate changeUpdate = changeId.update();
        return response.withHttpStatusCode(finalStatus.value())
                .withContent(responseContent)
                .withChangeId(changeUpdate.getAfter())
                .withLastChangeId(changeUpdate.getBefore());
    }

    private LibraryResponse responseWithNoChange(Object content) {
        return response.withHttpStatusCode(finalStatus.value())
                .withContent(content)
                .withChangeId(changeId.getChangeId());
    }

    public HttpStatus getFinalStatus() {
        return finalStatus;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }
}
