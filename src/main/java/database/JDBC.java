package database;

import java.sql.*;

public class JDBC {
    // test为数据库名称
    // MySQL 8.0 以上版本选择
    static final String JdbcDriver = "com.mysql.cj.jdbc.Driver";
    //连接服务器上的数据库
    static final String Url = "jdbc:mysql://47.106.102.237:3306/library?useSSL=false&serverTimezone=UTC";

    private static Connection conn = null;
    private static Statement stmt = null;
    private static ResultSet resultSet = null;
    private static String html = null;

    //输入连接数据库的用户名与密码(阿里云ECS)
    static final String User = "root";//输入你的数据库库名
    static final String PassWord = "CYH_library_1";//输入你的数据库连接密码

    public JDBC() {
        if(conn == null) startUp();
    }

    public void startUp(){
        // 注册 JDBC 驱动
        try {
            Class.forName(JdbcDriver);
            // 打开链接
//            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(Url, User, PassWord);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ResultSet getResultSet() {
        return resultSet;
    }

    public static String getHtml() {
        return html;
    }

    public static void setResultSet(ResultSet resultSet) {
        JDBC.resultSet = resultSet;
    }

    public boolean queryFromLibraryInfo(String groupId, String artifactId, String version) {
//        startUp();
        try {
            //SELECT count(*) from library where groupId = "com.google.guava" AND artifactId = "guava";
            String sql = "SELECT * from library_info where groupId = \"" + groupId  + "\""
                    +  " AND artifactId = \"" + artifactId  + "\"" +
                    " AND version = \"" + version + "\"" +  ";";

            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while(resultSet.next()) {
                html = resultSet.getString("content");
                res ++;
            }
            //获取结果集，如果为0说明不存在
            if (res == 0) return false;
            else return true;
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
//            try {
//                if (stmt != null) stmt.close();
//            } catch (SQLException se2) {
//            }
//            try {
//                if (conn != null) conn.close();
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }
        }
        return false;
    }

    public void insertIntoLibraryInfo(String groupId, String artifactId, String version, String javadoc, String content) {
        if(queryFromLibraryInfo(groupId, artifactId, version) == true) {
            System.out.println("数据库已存在该条数据！");
        }else{
            try {
                String html = content.replaceAll("\"","\"\"");

                String sql = "insert into library_info (groupId, artifactId, version, javadoc, content) values (\""  + groupId + "\", \"" + artifactId + "\", \"" + version + "\",\"" + javadoc + "\", \"" + html +"\" );";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet=stmt.executeUpdate(sql);
                if(resultSet > 0){
                    //如果插入成功，则打印success
                    System.out.println("Sucess");
                }else{
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            } finally {
//                // 关闭资源
//                try {
//                    if (stmt != null) stmt.close();
//                } catch (SQLException se2) {
//                }
//                try {
//                    if (conn != null) conn.close();
//                } catch (SQLException se) {
//                    se.printStackTrace();
//                }
            }
        }
    }

    /**
     * 查询数据库中是否存在该第三方库的网页源代码信息
     * @param groupId
     * @param artifactId
     * @return
     */
    public boolean queryFromLibraryContent(String groupId, String artifactId){
        try {
            //SELECT count(*) from library where groupId = "com.google.guava" AND artifactId = "guava";
            String sql = "SELECT * from library_content where groupId = \"" + groupId  + "\""
                    +  " AND artifactId = \"" + artifactId  +  "\";";

            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while(resultSet.next()) {
                //网页源代码赋值
                html = resultSet.getString("html");
                res ++;
            }
            //获取结果集，如果为0说明不存在
            if (res == 0) return false;
            else return true;
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
//            try {
//                if (stmt != null) stmt.close();
//            } catch (SQLException se2) {
//            }
//            try {
//                if (conn != null) conn.close();
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }
        }
        return false;
    }


    /**
     * 插入第三方库网页信息
     * @param groupId
     * @param artifactId
     * @param html
     */
    public void insertIntoLibraryContent(String groupId, String artifactId, String html){
        if(queryFromLibraryContent(groupId, artifactId) == true) {
            System.out.println("数据库已存在该条数据！");
        }else{
            try {
                html = html.replaceAll("\"","\"\"");

                String sql = "insert into library_content (groupId, artifactId, html) values (\""  + groupId + "\", \"" + artifactId + "\", \""  + html +"\" );";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet=stmt.executeUpdate(sql);
                if(resultSet > 0){
                    //如果插入成功，则打印success
                    System.out.println("Sucess");
                }else{
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            } finally {
//                // 关闭资源
//                try {
//                    if (stmt != null) stmt.close();
//                } catch (SQLException se2) {
//                }
//                try {
//                    if (conn != null) conn.close();
//                } catch (SQLException se) {
//                    se.printStackTrace();
//                }
            }
        }
    }

    /**
     * 修改该依赖的javadoc
     * @param groupId
     * @param artifactId
     * @param version
     */
    public void insertJavaDoc(String groupId, String artifactId, String version){

    }

    //最后关闭数据库资源
    public void closeOff(){
        // 关闭资源
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException se2) {
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JDBC jdbc = new JDBC();
        boolean isExist = jdbc.queryFromLibraryContent("org.dom4j", "dom4j");
        if(isExist) {
            String html = getHtml();
            System.out.println(html);
        }
    }


}
