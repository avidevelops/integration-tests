package in.av.qe.resttest.BOOKTests;

import in.av.qe.helpers.QaLogger;
import in.av.qe.vo.Book;
import in.av.qe.vo.ISBN;
import in.av.testcorelib.Utils;
import in.av.testcorelib.annotations.CreateTestIssue;
import in.av.testcorelib.annotations.IssueKey;
import in.av.testcorelib.extensions.ZephyrReferencesExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import static in.av.qe.api.Book.book;
import static in.av.qe.utils.InfrastructureConstants.userId;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ZephyrReferencesExtension.class)
public class BOOKTests {

    private static final Logger LOGGER = QaLogger.getQaLogger();

    @Test
    @IssueKey("TC-00")
    @CreateTestIssue
    void addBook() {
        String body = String.format(Utils.getResourceAsString("book/addBook.json"), userId);
        Book book = Utils.fromJson(body, Book.class);
        ISBN isbn = book().addListOfBooks(body);
        assertThat(isbn).isEqualTo(book.getCollectionOfIsbns().get(0).getIsbn());
    }
}
