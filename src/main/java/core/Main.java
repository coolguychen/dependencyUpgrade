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
        // TODO: 4/2/2023 调用第三方库依赖冲突调解程序
        procedure.upgradeProject();
        //用默认的结果集继续后续测试
//        procedure.defaultTest();
        //打印出结果集
//        procedure.printRes();
//        ---------------
    }

}
