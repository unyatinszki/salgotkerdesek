package unyat.salgot.question4.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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


    private final Logger logger = LoggerFactory.getLogger(ItemController.class);
    private final ItemService itemService;


    public ItemController(ItemService itemService) {
        logger.info("ItemController instantiated");
        this.itemService = itemService;
    }

    @PostMapping("/items")
    ResponseEntity<ItemsResponse> saveItems(@RequestBody ItemsRequest items) {

        logger.info("Processing request {}", items);
        return itemService.persistItems(items.getItems()).map(o -> {
            var response = new ItemsResponse(o);
            logger.info("Request completed successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }).orElseGet(() -> {
            logger.info("Request failed");
            return ResponseEntity.badRequest().build();
        });
    }

    @GetMapping("/items")
    List<ItemOutput> getAllItem() {
        return itemService.getAllItem();
    }
}
