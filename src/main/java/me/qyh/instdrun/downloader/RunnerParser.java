package me.qyh.instdrun.downloader;

import java.util.Map;

public class RunnerParser {

    private static final AntPathMatcher apm = new AntPathMatcher();
    private static final String[] POST_PATTERNS = new String[]{"https://www.instagram.com/p/{shortcode}/", "https://instagram.com/p/{shortcode}/"};
    private static final String[] USER_PATTERNS = new String[]{"https://www.instagram.com/{username}/", "https://instagram.com/{username}/"};
    private static final String[] LAST_STORY_PATTERNS = new String[]{"https://www.instagram.com/stories/{username}/{id}/", "https://instagram.com/stories/{username}/{id}/"};
    private static final String[] IGTV_PATTERNS = new String[]{"https://www.instagram.com/{username}/channel/", "https://instagram.com/{username}/channel/"};
    private static final String[] TAG_PATTERNS = new String[]{"https://www.instagram.com/explore/tags/{tagName}/", "https://instagram.com/explore/tags/{tagName}/"};

    private RunnerParser() {
        super();
    }

    public static DownloadRunner parse(String url) {
        String parseUrl = url;
        if (!parseUrl.endsWith("/")) {
            parseUrl = parseUrl + "/";
        }
        for (String postPattern : POST_PATTERNS) {
            if (apm.match(postPattern, parseUrl)) {
                String shortcode = apm.extractUriTemplateVariables(postPattern, parseUrl).get("shortcode");
                return new PostDownloader(shortcode);
            }
        }
        for (String userPattern : USER_PATTERNS) {
            if (apm.match(userPattern, parseUrl)) {
                String username = apm.extractUriTemplateVariables(userPattern, parseUrl).get("username");
                return new UserPostsDownloader(username);
            }
        }
        for (String lastStoryPattern : LAST_STORY_PATTERNS) {
            if (apm.match(lastStoryPattern, parseUrl)) {
                Map<String, String> variables = apm.extractUriTemplateVariables(lastStoryPattern, parseUrl);
                return new LastStoryDownloader(variables.get("username"), variables.get("id"));
            }
        }
        for (String igtvPattern : IGTV_PATTERNS) {
            if (apm.match(igtvPattern, parseUrl)) {
                Map<String, String> variables = apm.extractUriTemplateVariables(igtvPattern, parseUrl);
                return new UserIGTVDownloader(variables.get("username"));
            }
        }
        for (String tagPattern : TAG_PATTERNS) {
            if (apm.match(tagPattern, parseUrl)) {
                Map<String, String> variables = apm.extractUriTemplateVariables(tagPattern, parseUrl);
                return new TagPostsDownloader(variables.get("tagName"));
            }
        }
        return null;
    }

}
