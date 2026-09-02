package io.sj.saju.reading.dto;

import java.time.Instant;
import java.util.UUID;

/** One saved reading in a logged-in user's server-side history. */
public record ReadingHistoryEntry(UUID id, Instant createdAt, SajuReadingResult result) {
}
