# downins2

## 需要JDK11+

**使用了非官方的API，仅供娱乐使用**

## 下载某个帖子IGTV中的图片|视频

``` java
java -jar /path/to/jar https://www.instagram.com/p/{shortcode}
```

## 下载某个用户所有帖子中的图片|视频

``` java
java -jar /path/to/jar https://www.instangram.com/{username}
```

## 下载某个标签的全部图片|视频

``` java
java -jar /path/to/jar  https://www.instagram.com/explore/tags/{tagName}
```

## 下载某个用户的全部IGTV

``` java
java -jar /path/to/jar https://www.instagram.com/instagram/channel/
```

## 下载某个用户最近的快拍

``` java
java -jar /path/to/jar https://www.instagram.com/stories/{username}/{id}
```

## 下载某个用户的高亮story文件

``` java
java -jar /path/to/jar -hs username
```

## 打开设置面板

``` java
java -jar /path/to/jar -s
```

## 设置最新的query_hash

``` java
java -jar /path/to/jar -qh
```
