package com.veltro.inventory.service;

/**
 * Holds optional request context for audit records (B3-03).
 * 
 * <p>Username is NOT stored here 窶・it's retrieved from SecurityContextHolder
 * inside AuditCommandExecutor, consistent with VeltroAuditorAware pattern.
 * 
 * @param ipAddress Client IP address (nullable), captured from HttpServletRequest in controllers
 */
public record RequestAuditContext(String ipAddress) {
    
    /**
     * Creates an empty audit context with no IP address.
     * Used for operations not triggered by HTTP requests.
     */
    public static RequestAuditContext empty() {
        return new RequestAuditContext(null);
    }
    
    /**
     * Creates an audit context with the given IP address.
     * 
     * @param ipAddress the client IP address
     * @return audit context with IP
     */
    public static RequestAuditContext withIp(String ipAddress) {
        return new RequestAuditContext(ipAddress);
    }
}

