package unyat.salgot.question4.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import unyat.salgot.question4.dto.ItemInput;
import unyat.salgot.question4.repository.HashedItemRepository;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
public class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private HashedItemRepository itemRepository;

    @Test
    public void testPersistingItems() {
        var input = IntStream
                .range(0, 200)
                .mapToObj(i -> new ItemInput("item" + i, "password" + i))
                .collect(Collectors.toList());
        var output = itemService.persistItems("testTrackingId", input);
        output.toString();

    }

}
