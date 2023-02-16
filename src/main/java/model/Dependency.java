package model;

import core.Crawl;
import database.JDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static util.HttpUtil.LocalAddress;

/**
 * 依赖/第三方包的结构
 */
public class Dependency {
    private int id;
    private String groupId; //该依赖的groupId
    private String artifactId; //该依赖的artifactId
    private String version; //该依赖的当前版本
    private boolean conflict;
    private boolean duplicate;
    private int depth;
    private Dependency parentDependency;
    private List<String> higherVersions; //该依赖对应的更高版本
    private List<Dependency> higherList; //该依赖对应的更高依赖的集合
    //间接依赖
    private List<Dependency> subDependency;

    public List<String> getHigherVersions() {
        return higherVersions;
    }

    /**
     * 构造函数
     *
     * @param id               记为出现的顺序
     * @param groupId
     * @param artifactId
     * @param version
     * @param depth            深度
     * @param parentDependency 父依赖是谁
     */
    public Dependency(int id, String groupId, String artifactId, String version, int depth, Dependency parentDependency) {
        this.id = id;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.depth = depth;
        this.parentDependency = parentDependency;
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
        this.higherList = new ArrayList<>();
        this.higherVersions = new ArrayList<>();
        //把当前依赖的版本加进去
        this.higherVersions.add(version);
    }

    public Dependency() {

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

    public List<Dependency> getSubDependency() {
        return subDependency;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Dependency getParentDependency() {
        return parentDependency;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void printDependency() {
        System.out.println(getGroupId() + ":" + getArtifactId() + ":" + getVersion());
    }

    /**
     * 对于依赖d，获取它更高版本的集合
     *
     * @return higherSet
     * @throws InterruptedException
     */
    public List<Dependency> getHigherDependencyList() throws InterruptedException {
        // TODO: 7/2/2023 用selenium爬取网页信息
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String html = null;
                // TODO: 16/2/2023 首先看数据库是否存在该第三方库的信息
                JDBC jdbc = new JDBC();
                boolean isExist = jdbc.queryFromLibraryContent(getGroupId(), getArtifactId());
                //如果数据库中存在，则不用爬取
                if (isExist) {
                    html = jdbc.getHtml();
                } else { //否则用selenium爬取网页信息后插入本地数据库。
                    //获取到mvnrepository上的依赖的网址
                    String address = LocalAddress + getGroupId() + "/" + getArtifactId();
                    Crawl crawl = new Crawl();
                    //用selenium爬取网页
                    html = crawl.getPageSource(address);
                    //插入本地数据库
                    jdbc.insertIntoLibraryContent(getGroupId(), getArtifactId(), html);
                }
                //如果页面返回response不为null 说明响应成功 才能继续
                if (html != null) {
                    Document doc = Jsoup.parse(html);
                    //根据class标签名获取到版本号
                    for (Element e : doc.getElementsByClass("vbtn")) {
                        String text = e.text();
                        //如何判断是更高的依赖？——如果 version(当前版本) <= text 且字符串长度小于等于
                        if (version.length() <= text.length() && version.compareTo(text) < 0) {
                            //加入集合中
                            higherVersions.add(text);
                            higherList.add(new Dependency(groupId, artifactId, text));
                            System.out.println("获取到" + artifactId + "的更高版本:" + text);
                        } else {
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


    /**
     * 依赖的传递依赖
     *
     * @return 该依赖的全部传递依赖
     */
    public void getTransitiveDeps(DependencyTree dpTree) {
        String html = null;
        // TODO: 28/11/2022 判断该依赖是否存在于数据库中，如果存在可以直接读取其html
        JDBC jdbc = new JDBC();
        boolean res = jdbc.queryFromLibraryInfo(groupId, artifactId, version);
        //如果在数据库中存在
        if (res == true) {
            html = jdbc.getHtml();
        } else {
            //获取到mvnrepository上的依赖的网址
            String address = LocalAddress + getGroupId() + "/" + getArtifactId() + "/" + getVersion();
            try {
                html = HttpUtil.getHttp(address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //如果页面返回response不为null 说明解析html 才能继续
        //解析html得到传递依赖
        if (html != null) {
            Document doc = Jsoup.parse(html);
            //获取传递依赖 compile dependencies 直至为0 退出
            //获取tag为h2的，
            Elements elements = doc.getElementsByClass("version-section");
            //获取到compile dependencies
            Element e = elements.get(0);
            int num = getCompileDepsNum(e);
            if (num != 0) {
                //将传递依赖加入subDependency
                subDependency = getCompileDeps(e);
                List<DependencyTree> subTree = new ArrayList<>();
                //递归获取传递依赖
                for (Dependency d : subDependency) {
                    DependencyTree tree = new DependencyTree(d);
                    subTree.add(tree);
                    d.getTransitiveDeps(dpTree);
                }
                dpTree.setChildList(subTree); //设置子树
            } else { //若num为0 退出
                System.out.println(getArtifactId() + "的传递依赖不存在");
                return; //递归终止条件
            }
        } else {
            System.out.println("获取网页失败，请重试！");
            getTransitiveDeps(dpTree);
        }

    }

    public List<Dependency> getCompileDeps(Element e) {
        List<Dependency> subDependency = new ArrayList<>();
        Elements trs = e.getElementsByTag("tr");
        for (int i = 1; i < trs.size(); i++) {
            Element td = trs.get(i);
            Elements tds = td.getElementsByTag("td");
            //groupId & artifactId在td[2]
            Element info = tds.get(2);
            Elements idInfo = info.select("a[href]");
            String groupId = idInfo.get(0).text();
            String artifactId = idInfo.get(1).text();
            //version -- td[3]
            Element verElement = tds.get(3);
            String version = verElement.text();
            //获取其传递依赖
            Dependency dependency = new Dependency(groupId, artifactId, version);
            //加入集合中
            subDependency.add(dependency);
        }
        return subDependency;
    }

    /**
     * 获取传递依赖的数量
     *
     * @param e
     * @return
     */
    public int getCompileDepsNum(Element e) {
        Elements elements = e.getElementsByTag("h2");
        String eText = elements.get(0).text();
        int index = eText.indexOf("(");
        String num = eText.substring(index + 1, eText.length() - 1);
        return Integer.parseInt(num);
    }
}
