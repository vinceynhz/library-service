package app.tandv.services.controller;

import app.tandv.services.exception.LibraryOperationException;
import app.tandv.services.model.request.book.*;

import app.tandv.services.model.response.LibraryResponse;
import app.tandv.services.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Vic on 8/28/2018
 **/
@RestController
@CrossOrigin
public class LibraryController {
    private final LibraryService libraryService;

    @Autowired
    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * @return {@code 200 - OK} and true if service is up
     */
    @GetMapping(path = "/isRunning")
    public Boolean isRunning() {
        return true;
    }

    /*
    BOOK COLLECTION
     */

    /**
     * To retrieve the collection of books
     *
     * @return {@code 200 - OK} and the books (or empty object if the db is empty)
     * @throws LibraryOperationException {@code 500 - INTERNAL SERVER ERROR} if failed to retrieve the books
     */
    @GetMapping(path = "/books")
    public ResponseEntity<LibraryResponse> getBooks()
    throws LibraryOperationException {
        LibraryResponse response = libraryService.getBooks();
        return serviceResponse(response);
    }

    /**
     * To add a book to the collection
     *
     * @param request book to add
     * @return {@code 201 - CREATED} and the delta of changes: book and author(s) affected
     * @throws LibraryOperationException {@code 400 - BAD REQUEST} if authors in new book are not defined,
     *                                   {@code 409 - CONFLICT} if the book already exists
     *                                   {@code 500 - INTERNAL SERVER ERROR} if failed to add the book
     */
    @PostMapping(path = "/books")
    public ResponseEntity<LibraryResponse> postBook(@RequestBody AddBookRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.addBook(request);
        return serviceResponse(response);
    }

    /*
    BOOK RESOURCE
     */

    /**
     * @param bookId to retrieve
     * @return {@code 200 - OK} and the book information for given id
     * @throws LibraryOperationException {@code 404 - NOT FOUND} if no book with given id exists in the DB
     */
    @GetMapping(path = "/books/{bookId}")
    public ResponseEntity<LibraryResponse> getBook(@PathVariable Integer bookId)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.getBook(bookId);
        return serviceResponse(response);
    }

    /**
     * To replace a book with given information; this method internally delete the book with given id and adds the one
     * passed in the body of the request
     *
     * @param bookId  to replace
     * @param request new book information
     * @return {@code 200 - OK} and the delta of changes: book removed, book added and author(s) affected
     * @throws LibraryOperationException {@code 400 - BAD REQUEST} if authors on new book are not defined
     *                                   {@code 404 - NOT FOUND} if book with given id is not found in the library
     *                                   {@code 409 - CONFLICT} if new book exists already
     */
    @PutMapping(path = "/books/{bookId}")
    public ResponseEntity<LibraryResponse> putBook(@PathVariable Integer bookId, @RequestBody AddBookRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.updateBook(bookId, request);
        return serviceResponse(response);
    }

    /**
     * To update the quantity of the books available in the library
     *
     * @param bookId  to update
     * @param request changes to the quantity of books available in the library
     * @return {@code 200 - OK} and the delta of changes: book updated
     * @throws LibraryOperationException {@code 400 - BAD REQUEST} if quantity is set to 0 or a negative number
     *                                   {@code 404 - NOT FOUND} if book with given id is not found in the library
     *                                   {@code 406 - NOT ACCEPTABLE} if the action set in the request doesn't match one
     *                                   in {@link app.tandv.services.model.request.book.UpdateQtyActionEnum}
     */
    @PatchMapping(path = "/books/{bookId}/quantity")
    public ResponseEntity<LibraryResponse> patchBookQuantity(@PathVariable Integer bookId, @RequestBody UpdateBookQuantityRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.updateBookQuantity(bookId, request);
        return serviceResponse(response);
    }

    @PatchMapping(path = "/books/{bookId}/title")
    public ResponseEntity<LibraryResponse> patchBookTitle(@PathVariable Integer bookId, @RequestBody UpdateBookTitleRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.updateBookTitle(bookId, request);
        return serviceResponse(response);
    }

    /**
     * @param bookId to delete
     * @return {@code 200 - OK} and the delta of changes: book deleted, author(s) updated/error
     * @throws LibraryOperationException {@code 404 - NOT FOUND} if book with given id is not found in the library
     */
    @DeleteMapping(path = "/books/{bookId}")
    public ResponseEntity<LibraryResponse> deleteBook(@PathVariable Integer bookId)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.deleteBook(bookId);
        return serviceResponse(response);
    }


    /*
    AUTHORS BY BOOK RESOURCE
     */

    /**
     * @param bookId from which authors will be retrieved
     * @return {@code 200 - OK} and the list of authors of given book
     * @throws LibraryOperationException {@code 404 - NOT FOUND} if book with given id is not found in the library
     */
    @GetMapping(path = "/books/{bookId}/authors")
    public ResponseEntity<LibraryResponse> getAuthorsByBook(@PathVariable Integer bookId)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.getAuthorsByBook(bookId);
        return serviceResponse(response);
    }

    /**
     * @param bookId  to which author(s) will be added
     * @param request one or more authors to add to given book; author(s) will be sent by id if they exist or by name if
     *                they don't. In the case of adding by name, we'll still check for pre existent authors
     * @return {@code 200 - OK} and the delta of changes: book modified, author(s) affected, {@code 206 - PARTIAL RESPONSE}
     * if not all authors were processed, in which case the response will contain the delta of changes, and the failed
     * authors will include error messages
     * @throws LibraryOperationException {@code 400 - BAD REQUEST} if the list is empty
     *                                   {@code 404 - NOT FOUND} if book with given id is not found in the library
     */
    @PostMapping(path = "/books/{bookId}/authors")
    public ResponseEntity<LibraryResponse> postAuthorsToBook(@PathVariable Integer bookId, @RequestBody AddAuthorRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.addAuthorsToBook(bookId, request);
        return serviceResponse(response);
    }

    @PutMapping(path = "/books/{bookId}/authors")
    public ResponseEntity<LibraryResponse> putAuthorsToBook(@PathVariable Integer bookId, @RequestBody AddAuthorRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.updateBookAuthors(bookId, request);
        return serviceResponse(response);
    }

    /*
    AUTHORS BY BOOK RESOURCE
     */

    /**
     * @param bookId   from which the author will be removed
     * @param authorId to remove from the book
     * @return {@code 200 - OK} and the delta of changes: book updated, author(s) affected
     * @throws LibraryOperationException {@code 403 - FORBIDDEN} if the author indicated is the only one in the book
     *                                   {@code 404 - NOT FOUND} if either the book or the author are not find with
     *                                   given id
     */
    @DeleteMapping(path = "/books/{bookId}/authors/{authorId}")
    public ResponseEntity<LibraryResponse> deleteAuthorFromBook(@PathVariable Integer bookId, @PathVariable Integer authorId)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.deleteAuthorFromBook(bookId, authorId);
        return serviceResponse(response);
    }

    /*
    AUTHOR COLLECTION
     */

    /**
     * @return {@code 200 - OK} if successful, {@code 204 - NO CONTENT} if no books were available
     * @throws LibraryOperationException {@code 500 - INTERNAL SERVER ERROR} if failed to retrieve the authors
     */
    @GetMapping(path = "/authors")
    public ResponseEntity<LibraryResponse> getAuthors() throws LibraryOperationException {
        LibraryResponse response = libraryService.getAuthors();
        return serviceResponse(response);
    }

    /*
    AUTHOR RESOURCE
     */

    /**
     * @param authorId to retrieve
     * @return {@code 200 - OK} and the author information for given id
     * @throws LibraryOperationException {@code 404 - NOT FOUND} if no author with given id exists in the library
     */
    @GetMapping(path = "/authors/{authorId}")
    public ResponseEntity<LibraryResponse> getAuthor(@PathVariable Integer authorId)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.getAuthor(authorId);
        return serviceResponse(response);
    }

    /**
     * Internally, this method will insert the new author information, then all books matching the old's author id will
     * be updated, finally the old author will be removed.
     *
     * @param authorId to update
     * @param request  details of the author to update
     * @return {@code 200 - OK} and the delta of changes: the list of book(s) affected, the author deleted, and the
     * author added
     * @throws LibraryOperationException {@code 404 - NOT FOUND} if the author with given id is not found in the library
     *                                   {@code 409 - CONFLICT} if the new author information already exists
     *                                   {@code 500 - INTERNAL SERVER ERROR} for any other error
     */
    @PutMapping(path = "/authors/{authorId}")
    public ResponseEntity<LibraryResponse> putAuthor(@PathVariable Integer authorId, @RequestBody UpdateAuthorRequest request)
    throws LibraryOperationException {
        LibraryResponse response = libraryService.updateAuthor(authorId, request);
        return serviceResponse(response);
    }

    @GetMapping(path = "/admin/status")
    public ResponseEntity<LibraryResponse> getStatus(){
        LibraryResponse response = libraryService.getServiceStatus();
        return serviceResponse(response);
    }

    private ResponseEntity<LibraryResponse> serviceResponse(LibraryResponse response) {
        return new ResponseEntity<>(response, libraryService.getHttpHeaders(), libraryService.getFinalStatus());
    }
}
