package com.okx.trading;

import com.okx.trading.util.SystemUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * OKX交易API应用程序主类
 * 提供与OKX交易所API交互的功能
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class OkxTradingApplication {

    /**
     * 应用默认端口
     */
    private static final int DEFAULT_PORT = 8088;

    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            // 测试系统命令是否可用
            System.out.println("测试系统命令是否可用...");
            boolean commandTestResult = SystemUtil.testCommand();
            System.out.println("系统命令测试结果: " + (commandTestResult ? "成功" : "失败"));

            if (!commandTestResult) {
                System.out.println("警告: 系统命令测试失败，可能无法检查端口占用情况");
                System.out.println("应用将继续启动，但可能无法自动清理占用端口");
            } else {
                // 应用启动前检查并清理端口占用
                String portArg = getPortFromArgs(args);
                int port = portArg != null ? Integer.parseInt(portArg) : DEFAULT_PORT;

                System.out.println("启动前检查端口 " + port + " 占用情况...");
                String result = SystemUtil.checkAndKillPort(port);
                System.out.println("端口检查结果: " + result);
            }

            System.out.println("正在启动应用...");
        } catch (Exception e) {
            System.err.println("端口检查过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        // 启动Spring应用
        SpringApplication app = new SpringApplication(OkxTradingApplication.class);
        Environment env = app.run(args).getEnvironment();

        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info("\n----------------------------------------------------------\n" +
                "应用 '{}' 已成功启动! 访问URL:\n" +
                "本地: \thttp://localhost:{}{}\n" +
                "外部: \thttp://{}:{}{}\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name", "okx-trading"),
                port,
                contextPath,
                "127.0.0.1",
                port,
                contextPath);
    }

    /**
     * 从命令行参数中获取端口号
     *
     * @param args 命令行参数
     * @return 端口号字符串，如果未指定则返回null
     */
    private static String getPortFromArgs(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--server.port=")) {
                    return args[i].substring("--server.port=".length());
                } else if (args[i].equals("--server.port") && i + 1 < args.length) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }
}
