package me.qyh.instd4j.parser;

import me.qyh.instd4j.util.JsonExecutor;

import java.util.ArrayList;
import java.util.List;

import static me.qyh.instd4j.parser.InsParser.*;

class ParseUtils {

    private ParseUtils() {
        super();
    }

    public static Post parsePostNode(JsonExecutor node) {
        String id = node.execute("id").getAsString();
        String shortcode = node.execute("shortcode").getAsString();
        List<Link> thumbnails = new ArrayList<>();
        JsonExecutor thumbnailNodes = node.execute("thumbnail_resources");
        if (thumbnailNodes.isPresent()) {
            for (JsonExecutor thumbnailNode : thumbnailNodes) {
                thumbnails.add(new Link(thumbnailNode.execute("src").getAsString(), false));
            }
        }
        List<Link> links = new ArrayList<>();
        parseNode(node, links);
        String type = node.execute("__typename").getAsString();
        return new Post(type, shortcode, id, thumbnails, links);
    }

    public static IGTV parseIGTVNode(JsonExecutor node) {
        String id = node.execute("id").getAsString();
        String shortcode = node.execute("shortcode").getAsString();
        Link thumbnail = new Link(node.execute("thumbnail_src").getAsString(), false);
        double duration = node.execute("video_duration").getAsDouble();
        return new IGTV(shortcode, id, duration, thumbnail);
    }

    public static PageInfo parsePageInfoNode(JsonExecutor node) {
        boolean hasNextPage = node.execute("has_next_page").getAsBoolean();
        String endCursor = null;
        if (hasNextPage) {
            endCursor = node.execute("end_cursor").getAsString();
        }
        return new PageInfo(hasNextPage, endCursor);
    }

    public static Link parseStoryItemNode(JsonExecutor node) {
        if (node.execute("is_video").getAsBoolean()) {
            return new Link(node.execute("video_resources").last().execute("src").getAsString(), true);
        }
        return new Link(node.execute("display_url").getAsString(), false);
    }

    private static void parseNode(JsonExecutor node, List<Link> links) {
        String type = node.execute("__typename").getAsString();
        switch (type) {
            case GRAPH_SIDECAR:
                JsonExecutor children = node.execute("edge_sidecar_to_children->edges");
                if (children.isPresent()) {
                    for (JsonExecutor child : children) {
                        JsonExecutor childNode = child.execute("node");
                        parseNode(childNode, links);
                    }
                }
                break;
            case GRAPH_IMAGE:
                String url = node.execute("display_url").getAsString();
                links.add(new Link(url, false));
                break;
            case GRAPH_VIDEO:
                JsonExecutor videoUrlNode = node.execute("video_url");
                if (videoUrlNode.isPresent()) {
                    String videoUrl = node.execute("video_url").getAsString();
                    links.add(new Link(videoUrl, true));
                }
                break;
            default:
                throw new IllegalStateException("can not parse type : " + type);
        }
    }

    public static HighlightStory parseHighlightStoryNode(JsonExecutor node) {
        String id = node.execute("id").getAsString();
        String thumbnail = node.execute("cover_media->thumbnail_src").getAsString();
        String croppedThumbnail = node.execute("cover_media_cropped_thumbnail->url").getAsString();
        return new HighlightStory(id,new Link(thumbnail,false),new Link(croppedThumbnail,false));
    }
}
