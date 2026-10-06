package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite notification service coordinating in-app toasts and native Windows OS notifications.
 * Implements FR-04, FR-08, NFR-11, NFR-12.
 */
public class CompositeNotificationService implements NotificationService {

    private final List<NotificationService> delegates = new ArrayList<>();
    private NotificationActionListener currentListener;

    public CompositeNotificationService(NotificationService... services) {
        if (services != null) {
            for (NotificationService s : services) {
                if (s != null) {
                    delegates.add(s);
                }
            }
        }
    }

    public void addDelegate(NotificationService service) {
        if (service != null && !delegates.contains(service)) {
            delegates.add(service);
            if (currentListener != null) {
                service.setActionListener(currentListener);
            }
        }
    }

    @Override
    public void setActionListener(NotificationActionListener listener) {
        this.currentListener = listener;
        for (NotificationService delegate : delegates) {
            delegate.setActionListener(listener);
        }
    }

    @Override
    public void notifyTask(Task task, NagMessage message) {
        for (NotificationService delegate : delegates) {
            try {
                delegate.notifyTask(task, message);
            } catch (Throwable ignored) {
                // Fault isolation: one delegate failing does not prevent others
            }
        }
    }

    @Override
    public void dismissNotification(String taskId) {
        for (NotificationService delegate : delegates) {
            try {
                delegate.dismissNotification(taskId);
            } catch (Throwable ignored) {
            }
        }
    }
}
