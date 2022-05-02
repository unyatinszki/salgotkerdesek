package unyat.salgot.question4.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import unyat.salgot.question4.dao.HashedItem;
import unyat.salgot.question4.dto.ItemInput;
import unyat.salgot.question4.dto.ItemOutput;
import unyat.salgot.question4.repository.HashedItemRepository;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final HashedItemRepository repository;

    private final ExecutorService threadPool;

    private final MessageDigest digest;

    public ItemService(HashedItemRepository repository, @Value("${item.service.thread.pool.size:10}") int poolSize) {
        logger.info("ItemService instantiated - using thread pool size {}", poolSize);
        this.repository = repository;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down thread pool");
        threadPool.shutdown();
    }

    private CompletableFuture<Optional<ItemOutput>> persistItemAsync(String trackingId, ItemInput item) {
        return CompletableFuture.supplyAsync(
                () -> {
                    logger.info("{} Processing {}", trackingId, item);
                    byte[] encodedhash = digest.digest(item.getPassword().getBytes(StandardCharsets.UTF_8));
                    var base64EncodedHash = Base64.getEncoder().encodeToString(encodedhash);
                    HashedItem itemEntity = new HashedItem();
                    itemEntity.setId(UUID.randomUUID().toString());
                    itemEntity.setName(item.getName());
                    itemEntity.setPasswordHash(base64EncodedHash);
                    try {
                        repository.save(itemEntity);
                        ItemOutput result = new ItemOutput(item.getOrder(), itemEntity.getId(), itemEntity.getName(),
                                itemEntity.getPasswordHash());
                        logger.info("{} Successfully generated {}", trackingId, result);
                        return Optional.of(result);
                    } catch (RuntimeException e) {
                        logger.error(trackingId + " Saving item failed", e);
                        return Optional.empty();
                    }
                }, threadPool);
    }

    private Optional<ItemOutput> awaitFuture(String trackingId, CompletableFuture<Optional<ItemOutput>> future) {
        try {
            var res = future.get();
            logger.info("{} Future completed for {}", trackingId, res);
            return res;
        } catch (InterruptedException | ExecutionException e) {
            logger.error(trackingId + " Exception while evaluating future", e);
            return Optional.<ItemOutput>empty();
        }
    }

    private void populateOrderInfo(List<ItemInput> items) {
        // need to ensure the order as well - adding this information here
        int counter = 0;
        for (var item : items) {
            item.setOrder(counter++);
        }
    }

    /**
     * Persists InputItem-s into the DB in parallel on a thread pool.
     *
     * @param items the InputItems to be persisted
     * @return An empty optional if there were any errors (in this case the successful writes are deleted)
     * or the completed results in the same order as the input was.
     */
    public Optional<List<ItemOutput>> persistItems(String trackingId, List<ItemInput> items) {
        logger.info("{} Starting to process {} items", trackingId, items.size());
        populateOrderInfo(items);
        // creating futures for each item on the thread pool
        var futures = items
                .stream()
                .map(item -> persistItemAsync(trackingId, item))
                .collect(Collectors.toList());
        var successOutputs = futures
                .stream()
                .map(future -> awaitFuture(trackingId, future))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(ItemOutput::getOrder))
                .collect(Collectors.toList());
        var noError = successOutputs.size() == items.size();
        if (noError) {
            return Optional.of(successOutputs);
        } else {
            // need to clean up - need to delete the writes those were successful
            for (var output : successOutputs) {
                try {
                    repository.deleteById(output.getId());
                } catch (RuntimeException e) {
                    // catching anything that the repo might throw - at this stage we will return a http 400 anyway
                    logger.error("Exception while cleaning up after errors", e);
                }
            }
            return Optional.empty();
        }
    }

    public List<ItemOutput> getAllItem() {
        return repository
                .findAll()
                .stream()
                .map(i -> new ItemOutput(0, i.getId(), i.getName(), i.getPasswordHash()))
                .collect(Collectors.toList());
    }

}
