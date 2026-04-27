package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

/**
 * Template Method base class for the Alert Chain of Responsibility pattern.
 *
 * <p>Subclasses only need to implement {@link #evaluate(StockAlertEvaluationContext)},
 * which returns {@code true} when the specific alert condition is met,
 * and {@link #getAlertType()}, {@link #getAlertSeverity()}, and {@link #buildMessage(StockAlertEvaluationContext)}
 * to supply the alert details.
 *
 * <p>The chain-forwarding boilerplate ({@code setNext / handle → next.handle}) lives here once.
 */
public abstract class AbstractAlertHandler implements AlertHandler {

    private AlertHandler next;

    @Override
    public void setNext(AlertHandler handler) {
        this.next = handler;
    }

    @Override
    public void handle(StockAlertEvaluationContext context) {
        if (evaluate(context)) {
            AlertEntity alert = new AlertEntity();
            alert.setType(getAlertType());
            alert.setSeverity(getAlertSeverity());
            alert.setMessage(buildMessage(context));
            context.addAlert(alert);
        }
        if (next != null) {
            next.handle(context);
        }
    }

    /**
     * Evaluates whether this handler's alert condition is met.
     *
     * @param context the stock evaluation context
     * @return {@code true} if the alert should fire
     */
    protected abstract boolean evaluate(StockAlertEvaluationContext context);

    /** @return the alert type emitted by this handler */
    protected abstract AlertType getAlertType();

    /** @return the severity level for this handler's alerts */
    protected abstract AlertSeverity getAlertSeverity();

    /**
     * Builds the human-readable alert message.
     *
     * @param context the stock evaluation context
     * @return the alert message string
     */
    protected abstract String buildMessage(StockAlertEvaluationContext context);
}
