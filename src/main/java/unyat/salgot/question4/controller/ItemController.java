package unyat.salgot.question4.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unyat.salgot.question4.dto.ItemOutput;
import unyat.salgot.question4.dto.ItemsRequest;
import unyat.salgot.question4.dto.ItemsResponse;
import unyat.salgot.question4.service.ItemService;

import java.util.List;

@RestController
public class ItemController {
    public static final String TRACKING_ID_HEADER_NAME = "X-Tracking-Id";

    private final Logger logger = LoggerFactory.getLogger(ItemController.class);
    private final ItemService itemService;


    public ItemController(ItemService itemService) {
        logger.info("ItemController instantiated");
        this.itemService = itemService;
    }

    @PostMapping("/items")
    ResponseEntity<ItemsResponse> saveItems(@RequestHeader(TRACKING_ID_HEADER_NAME) String trackingId, @RequestBody ItemsRequest items) {
        logger.info("{} Processing request {}", trackingId, items);
        return itemService.persistItems(trackingId, items.getItems()).map(o -> {
            var response = new ItemsResponse(o);
            logger.info("{} Request completed successfully", trackingId);
            return ResponseEntity.status(HttpStatus.CREATED).header(TRACKING_ID_HEADER_NAME, trackingId).body(response);
        }).orElseGet(() -> {
            logger.info("{} Request failed", trackingId);
            return ResponseEntity.badRequest().header(TRACKING_ID_HEADER_NAME, trackingId).build();
        });
    }

    @GetMapping("/items")
    List<ItemOutput> getAllItem() {
        return itemService.getAllItem();
    }
}
