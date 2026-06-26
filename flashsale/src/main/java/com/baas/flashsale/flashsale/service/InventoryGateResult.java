package com.baas.flashsale.flashsale.service;

public record InventoryGateResult(Status status, int remainingQuantity) {
    public enum Status {
        RESERVED,
        OUT_OF_STOCK,
        ALREADY_PURCHASED
    }
}
