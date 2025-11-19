package com.vinodh.repository;

import com.vinodh.entity.SNMPNotificationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SNMPNotificationRepository extends MongoRepository<SNMPNotificationEntity, String> {
}
