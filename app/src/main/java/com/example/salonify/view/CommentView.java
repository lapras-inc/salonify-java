package com.example.salonify.view;

import com.example.salonify.entity.Comment;

import java.util.List;

/** board/thread.html のコメントノード（ネストした返信を含む）用の view model。 */
public record CommentView(Comment comment, String author, String bodyHtml, List<CommentView> replies) {
}
