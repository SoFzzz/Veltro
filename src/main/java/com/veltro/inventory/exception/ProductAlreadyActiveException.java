package com.veltro.inventory.exception;

public class ProductAlreadyActiveException extends RuntimeException {

    public ProductAlreadyActiveException(Long productId) {
        super("Product is already active: " + productId);
    }
}

