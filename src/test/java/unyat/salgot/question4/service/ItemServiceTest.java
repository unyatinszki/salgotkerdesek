package unyat.salgot.question4.service;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import unyat.salgot.question4.controller.TrackingIdFilter;
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
        MDC.put(TrackingIdFilter.MDC_TRACKING_ID_NAME, "testTrackingId");
        try {
            var input = IntStream
                    .range(0, 200)
                    .mapToObj(i -> new ItemInput("item" + i, "password" + i))
                    .collect(Collectors.toList());
            var output = itemService.persistItems(input);
            // TODO: asserts to be added. For the sake of the exercise i did manual testing via curl
            // but within normal circumstances i would do proper automated testing both at the service
            // as well as at the level of the controller
        } finally {
            MDC.clear();
        }
    }

}
