package core;

/**
 * Main函数
 */
public class Main {

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
        procedure.getHigherVersions();
        //获取最终的结果集
        procedure.getResults();
        //打印出结果集
        procedure.printRes();
        // TODO: 9/11/2022 冲突检测&筛选
        procedure.conflictDetect();
    }

}
