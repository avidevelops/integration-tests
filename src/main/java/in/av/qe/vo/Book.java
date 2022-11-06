package in.av.qe.vo;

import lombok.Data;

import java.util.List;

@Data
public class Book {
    private String userId;
    private List<ISBN> collectionOfIsbns;
}
