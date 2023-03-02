package core;

import java.util.Scanner;

/**
 * Main函数
 */
public class Main {

    public static void main(String[] args) {
// for test:  args = new String[]{"D:\\1javawork\\multiModelDemo"};
        // 初始化
        Init init = new Init();
        if(args.length != 0) {
            String str = args[0]; //执行jar包命令 传入参数
            init.inputPath(str);
        }
        else {
            // 输入项目路径
            init.inputPath();
        }
        // 创建一个解析的过程体
        Procedure procedure = new Procedure(init);
        // 判断项目类型
        procedure.checkType();
        // 调用升级第三方库的程序 &第三方库依赖冲突调解程序
        procedure.upgradeAndMediateProject();
        // 用默认的结果集继续后续测试
//        procedure.defaultTest();
        // 打印出结果集
//        procedure.printRes();
    }

}
