package com.myou.ec.ecsite.custom;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StructuredLoggingCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        // UTCをJSTに変更
        members.applyingValueProcessor(JsonWriter.ValueProcessor.of(instant -> {
                    OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant((Instant) instant, ZoneId.systemDefault());
                    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
                })
                .whenHasPath("@timestamp"));
    }
}
