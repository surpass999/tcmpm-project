package cn.gemrun.base.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 * 同时支持时间戳（毫秒）和字符串格式（如 "2026-03-19 04:44:55"）
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        // 尝试解析为字符串格式
        if (value != null && !value.isEmpty()) {
            try {
                return LocalDateTime.parse(value, DATETIME_FORMATTER);
            } catch (DateTimeParseException e) {
                // 字符串格式解析失败，尝试时间戳
            }
        }

        // 尝试解析为时间戳（毫秒）
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getValueAsLong()), ZoneId.systemDefault());
        } catch (Exception e) {
            throw new IOException("无法解析日期时间值: " + value, e);
        }
    }

}
