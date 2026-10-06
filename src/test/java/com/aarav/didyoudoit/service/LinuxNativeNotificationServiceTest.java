package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LinuxNativeNotificationServiceTest {

    @Test
    @DisplayName("LinuxNativeNotificationService dispatches safely with fault tolerance")
    void testLinuxNotificationDispatch() {
        LinuxNativeNotificationService service = new LinuxNativeNotificationService();
        assertNotNull(service);

        Task task = Task.builder().title("Push Code").build();
        NagMessage msg = new NagMessage("Due Now", "Push your changes!", EscalationLevel.WARN);

        // Even on Windows or headless environment where notify-send is absent, it must not throw
        assertDoesNotThrow(() -> {
            service.notifyTask(task, msg);
            service.dismissNotification(task.getId());
        });
    }
}
