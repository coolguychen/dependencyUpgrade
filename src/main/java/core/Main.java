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
        //创建一个解析的过程体
        Procedure procedure = new Procedure(init);
        procedure.checkType();
        // TODO: 4/2/2023 调用升级第三方库的程序
        procedure.upgradeProject();

        // TODO: 4/2/2023 调用第三方库依赖冲突调解程序

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


        // TODO: 3/2/2023 Maven多模块第三方库的升级
        // TODO: 16/1/2023 Maven多模块依赖冲突检测
        procedure.conflictDetect();

    }

}
