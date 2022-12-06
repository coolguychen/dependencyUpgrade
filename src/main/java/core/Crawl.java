package core;

import database.JDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import util.HttpUtil;
import util.RandomUtil;

import java.io.IOException;
import java.util.List;

import static util.HttpUtil.LocalAddress;

public class Crawl {
    private static String local = "https://mvnrepository.com/artifact/";
    //常用库的地址
    private static String[] address = {
            "commons-io/commons-io",
            "mysql/mysql-connector-java"
    };


    public static void startCrawl() {
        for (String link : address) {
            int index = link.lastIndexOf("/");
            String groupId = link.substring(0, index);
            String artifactId = link.substring(index + 1);
            String webLink = local + link;
            getPageByWebDriver(groupId, artifactId, webLink);
        }
        System.out.println("结束爬取！");
    }

    public static void getPageByWebDriver(String groupId, String artifactId, String webLink) {
        //设置驱动
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver(2).exe");
        //创建驱动
        WebDriver driver = new ChromeDriver();
        //与将要爬取的网站建立连接
        driver.get(webLink);
        List<WebElement> elements = driver.findElements(By.className("vbtn"));
        int size = elements.size();
        //爬取最新的几个版本
        for (int i = 0; i < Math.min(size, 10); i++) {
            String version = elements.get(i).getText();
            getSubPageByWebDriver(groupId, artifactId, version);
        }
        driver.close(); //关闭当前页面
    }

    private static void getSubPageByWebDriver(String groupId, String artifactId, String version) {
        sleep();
        String address = local + groupId + "/" + artifactId + "/" + version;
        //创建驱动
        WebDriver driver = new ChromeDriver();
        //与将要爬取的网站建立连接
        driver.get(address);
        //获取网页源代码
        String html = driver.getPageSource();
        JDBC jdbc = new JDBC();
        jdbc.insert(groupId, artifactId, version, null, html);
        driver.close();
    }


    public static void crawlMVN() {
        for (String link : address) {
            String webLink = local + link;
            getWeb(webLink);
        }
    }

    private static void getWeb(String webLink) {
        int index = webLink.lastIndexOf("/");
        String groupId = webLink.substring(0, index);
        String artifactId = webLink.substring(index);
        String html = null;
        try {
            html = HttpUtil.getHttp(webLink);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (html != null) {
            Document doc = Jsoup.parse(html);
            getLatestVersions(groupId, artifactId, doc);
        } else {
            System.out.println("网页爬取失败，请重试！");
            getWeb(webLink);
        }
    }

    public static void getLatestVersions(String groupId, String artifactId, Document doc) {
        sleep();
        //根据class标签名获取到版本号
        Elements elements = doc.getElementsByClass("vbtn");
        for (int i = 0; i < 10; i++) {
            String version = elements.get(i).text();
            String address = LocalAddress + groupId + "/" + artifactId + "/" + version;
            String html = null;
            try {
                html = HttpUtil.getHttp(address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sleep() {
        try {
            Thread.sleep(new RandomUtil().getRandomNum());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        startCrawl();
    }
}
