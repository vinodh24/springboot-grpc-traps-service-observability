package com.vinodh.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "snmp_notifications")
public class SNMPNotificationEntity {
    @Id
    private String id;
    private String deviceId;
    private String oid;
    private String value;
    private String timestamp;
    private boolean processed;
    private String errorMessage;

    // 🟢 Default constructor (required by Spring Data)
    public SNMPNotificationEntity() {}

    // 🟢 Parameterized constructor (used in your service)
    public SNMPNotificationEntity(String deviceId, String oid, String value,
                                  String timestamp, boolean processed, String errorMessage) {
        this.deviceId = deviceId;
        this.oid = oid;
        this.value = value;
        this.timestamp = timestamp;
        this.processed = processed;
        this.errorMessage = errorMessage;
    }

    // 🟢 Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getOid() { return oid; }
    public void setOid(String oid) { this.oid = oid; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

