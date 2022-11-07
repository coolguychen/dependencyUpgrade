package core;

/**
 * Main函数
 */
public class Main {
    //传入工程路径
    private String filePath;

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Init init = new Init();
                if(filePath != null){
                    init.setFilePath(filePath);
                } else{
                    init.inputPath();
                }
                init.execIt();

            }
        }).start();
    }

    public static void main(String[] args) {
        //初始化
        Init init = new Init();
        //输入项目路径
        init.inputPath();
//        init.execIt();
        //创建一个解析的过程体
        Procedure procedure = new Procedure(init);
        //解析pom文件
        procedure.parsePom();
        //对于每一个依赖，向上搜索
        procedure.upgradeVersion();
        //获取最终的结果集
        procedure.getResult();
    }
}
