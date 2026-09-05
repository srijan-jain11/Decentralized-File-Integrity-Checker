package com.integrity.alert;

import java.util.ArrayList;
import java.util.List;

/**
 * Subject in the Observer pattern: keeps a registry of
 * {@link AlertListener}s and notifies all of them whenever the
 * detection pipeline raises an {@link AlertEvent}.
 *
 * <p>Decoupling "something is wrong" (detection) from "what to do
 * about it" (delivery) is what lets this project add new alert
 * channels later without changing {@code TamperDetector}.</p>
 */
public class AlertService {

    private final List<AlertListener> listeners = new ArrayList<>();

    public void subscribe(AlertListener listener) {
        listeners.add(listener);
    }

    public void publish(AlertEvent event) {
        for (AlertListener listener : listeners) {
            listener.onAlert(event);
        }
    }
}
