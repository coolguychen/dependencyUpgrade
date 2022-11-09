package model;

import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import util.HttpUtil;
import util.RandomUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static util.HttpUtil.LocalAddress;

/**
 * 依赖/第三方包的结构
 */
public class Dependency {
    private String groupId; //该依赖的groupId
    private String artifactId; //该依赖的artifactId
    private String version; //该依赖的当前版本
    private List<String> higherVersions; //该依赖对应的更高版本
//    private DependencySet higherSet;
    private List<Dependency> higherList;


    public List<String> getHigherVersions() {
        return higherVersions;
    }

    /**
     * 构造函数
     *
     * @param groupId
     * @param artifactId
     * @param version
     */
    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
//        this.higherSet = new DependencySet();
        this.higherList = new ArrayList<>();
        this.higherVersions = new ArrayList<>();
        //把当前依赖的版本加进去
        this.higherVersions.add(version);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    //获取该依赖更高版本

    /**
     * 对于依赖d，获取它更高版本的集合
     * @return higherSet
     * @throws InterruptedException
     */
    public List<Dependency> getHigherDependencyList() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //线程休眠
                sleep();
                //获取到mvnrepository上的依赖的网址
                String address = LocalAddress + getGroupId() + "/" + getArtifactId();
                System.out.println("爬取" + address);
                Response response = HttpUtil.getHttp(address);
                //如果页面返回response不为null 说明响应成功 才能继续
                if (response != null) {
                    String html = null;
                    try {
                        html = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Document doc = Jsoup.parse(html);
                    //根据class标签名获取到版本号
                    for (Element e : doc.getElementsByClass("vbtn")) {
                        String text = e.text();
                        // TODO: 8/11/2022 如何判断版本更高还需考虑 从上至下的位置检索
                        //如何判断是更高的依赖？——如果 version(当前版本) <= text 且字符串长度小于等于
                        if (version.length() <= text.length() && version.compareTo(text) < 0 ) {
                            //加入集合中
                            higherVersions.add(text);
                            higherList.add(new Dependency(groupId, artifactId, text));
                            System.out.println("获取到" + artifactId + "的更高版本:" + text);
                        }
                        else{
                            //如果遇到了小于自身的依赖 退出循环？
                            break;
                        }
                    }
                    //最后要把当前依赖加进去，因为有可能不升级（大于等于）
                    higherList.add(new Dependency(groupId, artifactId, version));
                } else {
                    System.out.println("获取网页失败，请重试！");
                }
                latch.countDown();
            }
        }
        ).start();
        latch.await();
        return higherList;
    }

    private void sleep() {
        try {
            int randomNum = new RandomUtil().getRandomNum();
            Thread.sleep(randomNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
