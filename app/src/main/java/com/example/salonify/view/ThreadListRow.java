package com.example.salonify.view;

import com.example.salonify.entity.Thread;

/** 掲示板スレッド一覧（board/list.html）の行用の view model。 */
public record ThreadListRow(Thread thread, String author, long commentCount, long reactionCount) {
}
