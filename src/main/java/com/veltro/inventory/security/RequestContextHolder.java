package com.veltro.inventory.security;

/**
 * Thread-local holder for request context information.
 * 
 * <p>Stores request-scoped data (like client IP) that needs to be accessible
 * from service layer without passing HttpServletRequest through all layers.
 * 
 * <p>The filter {@code RequestContextFilter} populates this at the start of
 * each request and clears it at the end.
 * 
 * @see com.veltro.inventory.config.RequestContextFilter
 */
public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
        // Utility class
    }

    /**
     * Sets the request context for the current thread.
     * 
     * @param context the request context
     */
    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * Gets the request context for the current thread.
     * 
     * @return the request context, or null if not set
     */
    public static RequestContext get() {
        return CONTEXT.get();
    }

    /**
     * Clears the request context for the current thread.
     * Must be called at the end of each request to prevent memory leaks.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Gets the client IP address from the current request context.
     * 
     * @return the client IP address, or null if not available
     */
    public static String getClientIp() {
        RequestContext ctx = CONTEXT.get();
        return ctx != null ? ctx.clientIp() : null;
    }

    /**
     * Holds request-scoped context data.
     * 
     * @param clientIp the client IP address
     */
    public record RequestContext(String clientIp) {
    }
}
