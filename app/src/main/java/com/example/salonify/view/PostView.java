package com.example.salonify.view;

import com.example.salonify.entity.Post;

/** salon/posts.html と salon/home.html の投稿行用の view model。 */
public record PostView(Post post, boolean canView, String planName) {
}
