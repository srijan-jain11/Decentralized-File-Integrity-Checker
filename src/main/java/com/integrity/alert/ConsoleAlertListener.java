package com.integrity.alert;

/**
 * Simple {@link AlertListener} that prints a formatted line to the
 * console for every alert. This is the default listener wired up in
 * {@code Main}, but any number of additional listeners (file logger,
 * email notifier, etc.) could subscribe alongside it via
 * {@link AlertService}.
 */
public class ConsoleAlertListener implements AlertListener {

    @Override
    public void onAlert(AlertEvent event) {
        String icon = switch (event.getType()) {
            case FILE_MODIFIED -> "[MODIFIED]";
            case FILE_DELETED -> "[DELETED] ";
            case FILE_ADDED -> "[ADDED]   ";
            case ROOT_HASH_MISMATCH -> "[ALERT]   ";
        };
        System.out.println(icon + " " + event.getMessage());
    }
}
