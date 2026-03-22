package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义 LocalDateTime 反序列化器，支持时间戳和字符串格式
 */
@Slf4j
class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            // 尝试解析为时间戳（毫秒）
            long timestamp = Long.parseLong(value);
            return LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.ofHours(8));
        } catch (NumberFormatException e) {
            // 尝试解析为日期字符串 yyyy-MM-dd
            try {
                return LocalDateTime.parse(value + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                log.warn("[LocalDateTimeDeserializer] 日期解析失败: {}", value);
                return null;
            }
        }
    }
}
