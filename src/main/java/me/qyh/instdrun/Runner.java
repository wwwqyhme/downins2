package me.qyh.instdrun;

import me.qyh.instd4j.parser.InsParser;
import me.qyh.instd4j.parser.QueryHashTool;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;
import me.qyh.instdrun.downloader.DownloadRunner;
import me.qyh.instdrun.downloader.HighlightStoriesDownloader;
import me.qyh.instdrun.downloader.RunnerParser;

import java.util.Arrays;

public class Runner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printHelper();
            System.exit(0);
            return;
        }
        String first = args[0];
        if ("-s".equals(first)) {
            new SettingFrame();
            return;
        }
        if ("-query_hash".equals(first)
                || "-qh".equals(first)) {
            DowninsConfig config = Configure.get().getConfig();
            try(InsParser parser = new InsParser(config)) {
                QueryHashTool queryHashTool = parser.createQueryHashTool();
                System.out.println("正在查询query_hash，这将花费数分钟或者更长时间");
                config.setChannelQueryHash(queryHashTool.loadChannelQueryHash());
                config.setStoryQueryHash(queryHashTool.loadStoryQueryHash());
                config.setTagQueryHash(queryHashTool.loadTagPostsQueryHash());
                config.setUserQueryHash(queryHashTool.loadUserPostsQueryHash());
                config.setHighlightStoriesQueryHash(queryHashTool.loadHighlightStoriesQueryHash());
                config.store();
                System.out.println("保存成功");
            }
            return;
        }
        if ("-hs".equals(first)) {
            if (args.length == 1) {
                System.out.println("缺少用户名");
                System.exit(0);
                return;
            }
            String second = args[1];
            new HighlightStoriesDownloader(second).run(Arrays.copyOfRange(args, 2, args.length));
            return;
        }

        DownloadRunner runner = RunnerParser.parse(first);
        if (runner == null) {
            System.out.println("无法被解析的地址：" + first);
            System.exit(-1);
        } else {
            try {
                runner.run(Arrays.copyOfRange(args, 1, args.length));
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private static void printHelper() {
        String sb = "" + "通过 java -jar /path/to/jar url 下载对应的文件,例如：\r\n" +
                "        java -jar /path/to/jar https://www.instgram.com/instagram/ 将会下载用户instagram下的所有帖子文件\r\n" +
                "        java -jar /path/to/jar https://www.instgram.com/p/CDGg29YF490/ 将会下载帖子CDGg29YF490下的所有文件\r\n" +
                "        java -jar /path/to/jar https://www.instagram.com/instagram/channel/ 将会下载用户instagram下的所有IGTV文件\r\n" +
                "        java -jar /path/to/jar https://www.instagram.com/stories/instagram/2368439156631504682/ 将会下载用户instagram的最近的快拍文件\r\n" +
                "        java -jar /path/to/jar https://www.instagram.com/explore/tags/instagram/ 将会下载标签instagram下的所有帖子文件(实验性)\r\n" +
                "通过 java -jar /path/to/jar -hs username 下载用户高亮快拍\r\n" +
                "通过 java -jar /path/to/jar -qh 重新设置query_hash\r\n" +
                "通过 java -jar /path/to/jar -s 打开配置面板";
        System.out.println(sb);
    }
}
