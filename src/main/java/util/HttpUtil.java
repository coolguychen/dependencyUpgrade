package util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpUtil {
    //固定范式访问网址
    public static final String LocalAddress = "https://mvnrepository.com/artifact/";


    /**
     * 返回获取网页后得到的html string
     * @param address
     * @return
     */
    /**
     * 返回获取网页后得到的html string
     * @param address
     * @return
     */
    public static String getHttp(String address) throws IOException {
        String result = null;
        UserAgentUtil userAgentUtil = new UserAgentUtil();
        //随机获取一个user-agent
        String userAgent = userAgentUtil.getUserAgent();
        //创建httpClient实例
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //创建httpGet实例
        HttpGet httpGet = new HttpGet(address);
        System.out.println("正在爬取：" + address);
        httpGet.setHeader("User-Agent",userAgent);
//        httpGet.setHeader("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:0.9.4)");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        sleep(); //休眠一段时间
        int code = response.getStatusLine().getStatusCode();
        System.out.println("页面状态码：" + code);
        //状态返回码为200才可继续
        if (response != null && code == 200){
            HttpEntity entity =  response.getEntity();  //获取网页内容
            result = EntityUtils.toString(entity, "UTF-8");
//            System.out.println("网页内容:"+result);
        }
        if (response != null){
            response.close();
        }
        if (httpClient != null){
            httpClient.close();
        }
        return result;
    }


    public static void sleep() {
        try {
            int randomNum = new RandomUtil().getRandomNum();
            Thread.sleep(randomNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
