package com.integrity.alert;

/**
 * Observer interface for components that want to react whenever the
 * {@code TamperDetector} raises an {@link AlertEvent}. Implementations decide how to surface
 * the alert - to the console, a log file, an email, a webhook, etc.
 * New delivery channels can be added without touching detection logic
 * (Open/Closed Principle - supports the Maintainability requirement).
 */
public interface AlertListener {
    void onAlert(AlertEvent event);
}
