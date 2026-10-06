package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;
import com.aarav.didyoudoit.ui.components.ToastNotificationCard;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Dispatches notifications within the JavaFX application UI using the Sonner-style toaster.
 * Implements FR-04, FR-08, NFR-02, NFR-14.
 */
public class InAppNotificationService implements NotificationService {

    private NotificationActionListener actionListener;
    private Consumer<ToastNotificationCard> toastConsumer;
    private final Map<String, ToastNotificationCard> activeToasts = new ConcurrentHashMap<>();

    public InAppNotificationService() {
    }

    /**
     * Registers the UI consumer (typically the toast stack in DashboardView) to receive toasts.
     */
    public void setToastConsumer(Consumer<ToastNotificationCard> consumer) {
        this.toastConsumer = consumer;
    }

    @Override
    public void setActionListener(NotificationActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void notifyTask(Task task, NagMessage message) {
        if (task == null || message == null) {
            return;
        }

        Runnable displayAction = () -> {
            if (toastConsumer == null) {
                return;
            }

            // Dismiss existing toast for this task if active
            dismissNotification(task.getId());

            String title = message.title();
            String body = message.body();

            ToastNotificationCard.ToastActionListener toastListener = new ToastNotificationCard.ToastActionListener() {
                @Override
                public void onComplete() {
                    activeToasts.remove(task.getId());
                    if (actionListener != null) {
                        actionListener.onMarkDone(task.getId());
                    }
                }

                @Override
                public void onSnooze() {
                    activeToasts.remove(task.getId());
                    if (actionListener != null) {
                        actionListener.onSnooze(task.getId(), 15);
                    }
                }

                @Override
                public void onDismiss() {
                    activeToasts.remove(task.getId());
                }
            };

            ToastNotificationCard card = new ToastNotificationCard(title, body, toastListener);
            activeToasts.put(task.getId(), card);
            toastConsumer.accept(card);
        };

        if (Platform.isFxApplicationThread()) {
            displayAction.run();
        } else {
            Platform.runLater(displayAction);
        }
    }

    @Override
    public void dismissNotification(String taskId) {
        if (taskId == null) {
            return;
        }
        ToastNotificationCard card = activeToasts.remove(taskId);
        if (card != null) {
            Runnable dismissAction = () -> card.dismiss(null);
            if (Platform.isFxApplicationThread()) {
                dismissAction.run();
            } else {
                Platform.runLater(dismissAction);
            }
        }
    }
}
