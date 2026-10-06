package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.*;
import com.aarav.didyoudoit.repository.HistoryRepository;
import com.aarav.didyoudoit.repository.SettingsRepository;
import com.aarav.didyoudoit.repository.TaskRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clean, standard-library implementation of {@link DataBackupService} for JSON export/import.
 * Implements FR-25 without introducing external dependencies.
 */
public class JsonDataBackupServiceImpl implements DataBackupService {

    private static final Logger LOGGER = Logger.getLogger(JsonDataBackupServiceImpl.class.getName());

    private final TaskRepository taskRepository;
    private final SettingsRepository settingsRepository;
    private final HistoryRepository historyRepository;

    public JsonDataBackupServiceImpl(TaskRepository taskRepository,
                                     SettingsRepository settingsRepository,
                                     HistoryRepository historyRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository cannot be null");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository cannot be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository cannot be null");
    }

    @Override
    public void exportBackup(File targetFile) {
        Objects.requireNonNull(targetFile, "targetFile cannot be null");

        List<Task> tasks = taskRepository.findAll();
        AppSettings settings = settingsRepository.getSettings();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");

        // 1. Settings JSON
        sb.append("  \"settings\": {\n");
        sb.append("    \"personalityType\": \"").append(settings.getPersonalityType().name()).append("\",\n");
        sb.append("    \"quietHoursEnabled\": ").append(settings.isQuietHoursEnabled()).append(",\n");
        sb.append("    \"quietHoursStart\": \"").append(settings.getQuietHoursStart()).append("\",\n");
        sb.append("    \"quietHoursEnd\": \"").append(settings.getQuietHoursEnd()).append("\",\n");
        sb.append("    \"autoStartOnBoot\": ").append(settings.isAutoStartOnBoot()).append(",\n");
        sb.append("    \"escalationIntervalMinutes\": ").append(settings.getEscalationIntervalMinutes()).append(",\n");
        sb.append("    \"defaultSnoozeMinutes\": ").append(settings.getDefaultSnoozeMinutes()).append("\n");
        sb.append("  },\n");

        // 2. Tasks JSON
        sb.append("  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(t.getId()).append("\",\n");
            sb.append("      \"title\": \"").append(escapeJson(t.getTitle())).append("\",\n");
            sb.append("      \"description\": \"").append(escapeJson(t.getDescription())).append("\",\n");
            sb.append("      \"category\": \"").append(t.getCategory().name()).append("\",\n");
            sb.append("      \"priority\": \"").append(t.getPriority().name()).append("\",\n");
            sb.append("      \"status\": \"").append(t.getStatus().name()).append("\",\n");
            sb.append("      \"dueDateTime\": ").append(t.getDueDateTime() != null ? "\"" + t.getDueDateTime() + "\"" : "null").append(",\n");
            sb.append("      \"isTemplate\": ").append(t.isTemplate()).append(",\n");
            sb.append("      \"parentTemplateId\": ").append(t.getParentTemplateId() != null ? "\"" + t.getParentTemplateId() + "\"" : "null").append(",\n");
            sb.append("      \"customNagMessage\": ").append(t.getCustomNagMessage() != null ? "\"" + escapeJson(t.getCustomNagMessage()) + "\"" : "null").append("\n");
            sb.append("    }").append(i < tasks.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try {
            Files.writeString(targetFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            LOGGER.info("Exported backup data to " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write backup file", e);
            throw new RuntimeException("Could not export backup: " + e.getMessage(), e);
        }
    }

    @Override
    public void importBackup(File sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile cannot be null");
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("Backup file does not exist: " + sourceFile.getAbsolutePath());
        }

        try {
            String json = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);

            // 1. Import Settings
            AppSettings settings = settingsRepository.getSettings();
            String personalityStr = extractJsonField(json, "personalityType");
            if (personalityStr != null) {
                settings.setPersonalityType(PersonalityType.fromString(personalityStr));
            }
            String quietEnabledStr = extractJsonField(json, "quietHoursEnabled");
            if (quietEnabledStr != null) {
                settings.setQuietHoursEnabled(Boolean.parseBoolean(quietEnabledStr));
            }
            String quietStartStr = extractJsonField(json, "quietHoursStart");
            if (quietStartStr != null) {
                settings.setQuietHoursStart(LocalTime.parse(quietStartStr));
            }
            String quietEndStr = extractJsonField(json, "quietHoursEnd");
            if (quietEndStr != null) {
                settings.setQuietHoursEnd(LocalTime.parse(quietEndStr));
            }
            String autoStartStr = extractJsonField(json, "autoStartOnBoot");
            if (autoStartStr != null) {
                settings.setAutoStartOnBoot(Boolean.parseBoolean(autoStartStr));
            }
            String escalationStr = extractJsonField(json, "escalationIntervalMinutes");
            if (escalationStr != null) {
                settings.setEscalationIntervalMinutes(Integer.parseInt(escalationStr));
            }
            String snoozeStr = extractJsonField(json, "defaultSnoozeMinutes");
            if (snoozeStr != null) {
                settings.setDefaultSnoozeMinutes(Integer.parseInt(snoozeStr));
            }
            settingsRepository.saveSettings(settings);

            // 2. Import Tasks
            List<String> taskObjects = extractTaskObjects(json);
            for (String taskJson : taskObjects) {
                String id = extractJsonField(taskJson, "id");
                String title = extractJsonField(taskJson, "title");
                if (title == null || title.isBlank()) continue;

                String desc = extractJsonField(taskJson, "description");
                String catStr = extractJsonField(taskJson, "category");
                String priStr = extractJsonField(taskJson, "priority");
                String statStr = extractJsonField(taskJson, "status");
                String dueStr = extractJsonField(taskJson, "dueDateTime");
                String isTmplStr = extractJsonField(taskJson, "isTemplate");
                String parentId = extractJsonField(taskJson, "parentTemplateId");
                String customNag = extractJsonField(taskJson, "customNagMessage");

                Task.Builder builder = Task.builder()
                        .title(unescapeJson(title))
                        .description(desc != null ? unescapeJson(desc) : "")
                        .category(catStr != null ? TaskCategory.fromString(catStr) : TaskCategory.OTHER)
                        .priority(priStr != null ? Priority.valueOf(priStr) : Priority.MEDIUM)
                        .status(statStr != null ? TaskStatus.valueOf(statStr) : TaskStatus.PENDING)
                        .isTemplate(Boolean.parseBoolean(isTmplStr))
                        .parentTemplateId(parentId)
                        .customNagMessage(customNag != null ? unescapeJson(customNag) : null);

                if (id != null && !id.isBlank()) {
                    builder.id(id);
                }
                if (dueStr != null && !dueStr.isBlank() && !dueStr.equals("null")) {
                    builder.dueDateTime(LocalDateTime.parse(dueStr));
                }

                Task task = builder.build();
                taskRepository.save(task);
            }

            LOGGER.info("Imported backup from " + sourceFile.getAbsolutePath() + " with " + taskObjects.size() + " tasks.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read backup file", e);
            throw new RuntimeException("Could not import backup: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String unescapeJson(String text) {
        if (text == null) return null;
        return text.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    private String extractJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*(?:\"([^\"]*)\"|([^,\n}]*))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return matcher.group(1);
            } else if (matcher.group(2) != null) {
                return matcher.group(2).trim();
            }
        }
        return null;
    }

    private List<String> extractTaskObjects(String json) {
        List<String> list = new ArrayList<>();
        int tasksIdx = json.indexOf("\"tasks\"");
        if (tasksIdx == -1) return list;

        int startBracket = json.indexOf('[', tasksIdx);
        if (startBracket == -1) return list;

        int depth = 0;
        int objStart = -1;
        for (int i = startBracket + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    list.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return list;
    }
}
