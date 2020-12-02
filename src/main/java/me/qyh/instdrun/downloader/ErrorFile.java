package me.qyh.instdrun.downloader;

class ErrorFile {
    private String url;
    private String dest;
    private String postId;

    public ErrorFile() {
        super();
    }

    public ErrorFile(String url, String dest, String postId) {
        this.url = url;
        this.dest = dest;
        this.postId = postId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUrl() {
        return url;
    }

    public String getDest() {
        return dest;
    }

    public String getPostId() {
        return postId;
    }
}
