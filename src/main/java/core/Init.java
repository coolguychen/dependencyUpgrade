package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * 初始化类
 */
public class Init {
    private String filePath;
    private BufferedReader result;


    public void inputPath(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入项目根目录:");
        while (scanner.hasNextLine()) {
            filePath = scanner.nextLine();
//            System.out.println(filePath);
            if(!filePath.equals(""))
                break;
        }
        File file = new File(filePath + "/pom.xml");
        if(!file.exists()){
            System.out.println("该路径下无pom.xml");
            inputPath();
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setResult(BufferedReader result) {
        this.result = result;
    }

    //执行构造项目的依赖关系
    public void execIt(){
        try {
            Runtime runtime = Runtime.getRuntime();
            System.out.println("正在构造依赖关系");
            Process process = runtime.exec(new String[]{"cmd","/c","mvn dependency:tree>tree.txt -Dverbose"}, null, new File(filePath));
            int code = process.waitFor();
//                System.out.println(code);
            System.out.println("构造完毕");
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            BufferedReader errorBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
            String line = errorBuffer.readLine();
            if(code != 0 && line != null){
                System.out.println("出现异常:");
                do{
                    System.out.println(line);
                }while((line = errorBuffer.readLine()) != null);
            }else{
//                    result = inputBuffer;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BufferedReader getResult() {
        return result;
    }
}
