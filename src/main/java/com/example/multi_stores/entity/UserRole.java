package com.example.multi_stores.entity;

public enum UserRole {
    ADMIN,
    OWNER,
    CUSTOMER,
    /** Employee: can see all orders and update their delivery status, nothing else. */
    STAFF
}
