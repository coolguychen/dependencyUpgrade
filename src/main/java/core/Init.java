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

    public void inputPath(String path){
        this.filePath = path;
        File file = new File(path + "/pom.xml");
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

    public BufferedReader getResult() {
        return result;
    }
}
