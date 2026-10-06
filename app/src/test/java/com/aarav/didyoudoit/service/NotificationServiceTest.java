package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private static class MockNotificationService implements NotificationService {
        final List<Task> notified = new ArrayList<>();
        final List<String> dismissed = new ArrayList<>();
        NotificationActionListener listener;

        @Override
        public void setActionListener(NotificationActionListener listener) {
            this.listener = listener;
        }

        @Override
        public void notifyTask(Task task, NagMessage message) {
            notified.add(task);
        }

        @Override
        public void dismissNotification(String taskId) {
            dismissed.add(taskId);
        }
    }

    @Test
    @DisplayName("CompositeNotificationService broadcasts notifications and dismissals to all delegates")
    void testCompositeNotificationService() {
        MockNotificationService s1 = new MockNotificationService();
        MockNotificationService s2 = new MockNotificationService();

        CompositeNotificationService composite = new CompositeNotificationService(s1, s2);

        Task task = Task.builder().title("Drink Water").build();
        NagMessage msg = new NagMessage("Reminder", "Drink up!", EscalationLevel.INITIAL);

        composite.notifyTask(task, msg);

        assertEquals(1, s1.notified.size());
        assertEquals(1, s2.notified.size());

        composite.dismissNotification(task.getId());

        assertEquals(1, s1.dismissed.size());
        assertEquals(1, s2.dismissed.size());
    }

    @Test
    @DisplayName("CompositeNotificationService propagates action listeners to delegates")
    void testActionListenerPropagation() {
        MockNotificationService s1 = new MockNotificationService();
        MockNotificationService s2 = new MockNotificationService();

        CompositeNotificationService composite = new CompositeNotificationService(s1);

        AtomicBoolean markDoneCalled = new AtomicBoolean(false);
        NotificationService.NotificationActionListener listener = new NotificationService.NotificationActionListener() {
            @Override
            public void onMarkDone(String taskId) {
                markDoneCalled.set(true);
            }

            @Override
            public void onSnooze(String taskId, int minutes) {}

            @Override
            public void onOpenApp(String taskId) {}
        };

        composite.setActionListener(listener);
        composite.addDelegate(s2);

        assertNotNull(s1.listener);
        assertNotNull(s2.listener);

        s2.listener.onMarkDone("task-123");
        assertTrue(markDoneCalled.get());
    }
}
