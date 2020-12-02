package me.qyh.instdrun.downloader;

public class PostState {
    private String postId;
    private boolean complete;

    public PostState() {
    }

    public PostState(String postId, boolean complete) {
        this.postId = postId;
        this.complete = complete;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
