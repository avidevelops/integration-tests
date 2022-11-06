package in.av.qe.api;

import in.av.qe.utils.ServicePath;
import in.av.qe.vo.ISBN;
import in.av.qe.vo.Message;
import io.restassured.http.ContentType;

import static in.av.qe.user.QaUser.testUser;
import static in.av.qe.utils.ITRestAssured.createByPost;
import static in.av.qe.utils.ITRestAssured.deleteByDelete;

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
        return createByPost(rs, BOOKS, booksToAdd, ContentType.JSON, 201).extract().as(ISBN.class);
    }

    public Message deleteBook(String book) {
        return deleteByDelete(rs, BOOKS, book, 204).extract().as(Message.class);
    }
}
