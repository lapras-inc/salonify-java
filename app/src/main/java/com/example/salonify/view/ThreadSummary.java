package com.example.salonify.view;

import com.example.salonify.entity.Thread;

/** View model for a thread row in salon/home.html. */
public record ThreadSummary(Thread thread, long replyCount) {
}
