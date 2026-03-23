package in.av.qe.api;

import in.av.qe.utils.ServicePath;
import in.av.qe.vo.ISBN;
import in.av.qe.vo.Message;

import static in.av.qe.user.QaUser.testUser;
import static in.av.qe.utils.ITRestPlay.*;

public class Book extends MicroService {

    private static final String BOOKS = "/Books";
    private static final String BOOK = "/Book";

    public Book() {
        super(ServicePath.BOOKS);
    }

    public static Book book() {
        return testUser().calls().service(Book.class);
    }

    public ISBN addListOfBooks(String booksToAdd) {
        return extractObject(ISBN.class, createByPost(requestContext, BOOKS, booksToAdd, "application/json", 201));
    }

    public Message deleteBook(String book) {
        return extractObject(
                Message.class,
                deleteByDelete(requestContext, appPrefix() + BOOKS, book, 204)
        );
    }
}
