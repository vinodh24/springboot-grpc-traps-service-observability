package com.vinodh.controller;

import com.vinodh.entity.SNMPNotificationEntity;
import com.vinodh.repository.SNMPNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/notifications")
public class SNMPNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(SNMPNotificationController.class);

    @Autowired
    private SNMPNotificationRepository repository;

    @GetMapping
    public List<SNMPNotificationEntity> getAll() {
        logger.debug("Handling GET /api/notifications - fetch all notifications");
        List<SNMPNotificationEntity> all = repository.findAll();
        logger.info("Retrieved {} notifications", all.size());
        return all;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SNMPNotificationEntity> getById(@PathVariable String id) {
        logger.debug("Handling GET /api/notifications/{}", id);
        Optional<SNMPNotificationEntity> notification = repository.findById(id);
        if (notification.isPresent()) {
            logger.info("Notification {} found", id);
            return ResponseEntity.ok(notification.get());
        } else {
            logger.warn("Notification {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping
    public SNMPNotificationEntity create(@RequestBody SNMPNotificationEntity entity) {
        logger.debug("Handling POST /api/notifications - payload: {}", entity);
        entity.setProcessed(false);
        SNMPNotificationEntity saved = repository.save(entity);
        logger.info("Created notification with id={}", saved.getId());
        return saved;
    }

    @PatchMapping("/{id}/processed")
    public ResponseEntity<SNMPNotificationEntity> markProcessed(@PathVariable String id,
                                                                @RequestParam boolean processed) {
        logger.debug("Handling PATCH /api/notifications/{}/processed - processed={}", id, processed);
        Optional<SNMPNotificationEntity> notification = repository.findById(id);
        if (notification.isPresent()) {
            SNMPNotificationEntity existing = notification.get();
            existing.setProcessed(processed);
            repository.save(existing);
            logger.info("Marked notification {} as processed={}", id, processed);
            return ResponseEntity.ok(existing);
        } else {
            logger.warn("Cannot mark processed - notification {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.debug("Handling DELETE /api/notifications/{}", id);
        if (repository.existsById(id))
        {
            repository.deleteById(id);
            logger.info("Deleted notification {}", id);
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Delete failed - notification {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }
}