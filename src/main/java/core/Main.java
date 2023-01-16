package core;

/**
 * Main函数
 */
public class Main {

    public static void main(String[] args) {
        //初始化
        Init init = new Init();
        //输入项目路径
//        init.inputPath();
        //创建一个解析的过程体
        Procedure procedure = new Procedure(init);


        // TODO: 16/1/2023 单模块or多模块判断 
        
        //解析pom文件
//        procedure.parsePom();
        //对于每一个依赖，向上搜索
//        procedure.getHigherVersions();
        //获取最终的结果集
//        procedure.getResults();
        //用默认的结果集继续后续测试
        procedure.defaultTest();
        //打印出结果集
        procedure.printRes();
//        ---------------

        
        // TODO: 16/1/2023 Maven多模块依赖冲突检测 
        procedure.conflictDetect();

        // TODO: 3/1/2023 API兼容性判断 
    }

}
