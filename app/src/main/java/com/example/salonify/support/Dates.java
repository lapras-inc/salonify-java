package com.example.salonify.support;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Thymeleaf から ${@dates.date(...)} としてアクセスできる日付フォーマットヘルパー。 */
@Component("dates")
public class Dates {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZONE);

    public String date(Instant t) {
        return t == null ? "" : DATE.format(t);
    }

    public String dateTime(Instant t) {
        return t == null ? "" : DATETIME.format(t);
    }
}
