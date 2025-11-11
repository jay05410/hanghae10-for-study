package io.hhplus.ecommerce.common.util

enum class IdPrefix(val value: String) {
    ORDER("ORD"),
    PAYMENT("PAY"),
    TRANSACTION("TXN"),
    COUPON("CPN"),
    USER("USR"),
    PRODUCT("PRD"),
    INVENTORY("INV")
}