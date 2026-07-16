package com.example.gymcrm.exception;

public class EntityNotFoundException extends GymCrmException {
    public EntityNotFoundException(String entityName, String key) {
        super(entityName + " not found: " + key);
    }
}
