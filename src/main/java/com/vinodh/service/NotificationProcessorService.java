package com.vinodh.service;

import com.vinodh.entity.SNMPNotificationEntity;
import com.vinodh.repository.SNMPNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessorService.class);

    @Autowired
    private SNMPNotificationRepository repository;

    public void processNotification(String deviceId, String oid, String value, String timestamp) {
        log.info("✅ Processing notification for deviceId: {}", deviceId);
        // Here you can add custom validation, transformation, etc.
        SNMPNotificationEntity entity = new SNMPNotificationEntity(
                deviceId, oid, value, timestamp, true, null
        );
        log.info("💾 Saved notification for deviceId: {}", deviceId);

        long start = System.currentTimeMillis();
        repository.save(entity);
        long end = System.currentTimeMillis();
        log.info("Mongo save took {} ms", (end - start));

    }

    public void handleError(String deviceId, String oid, String value, String timestamp, String error) {
        SNMPNotificationEntity entity = new SNMPNotificationEntity(
                deviceId, oid, value, timestamp, false, error
        );
        repository.save(entity);
    }
}
