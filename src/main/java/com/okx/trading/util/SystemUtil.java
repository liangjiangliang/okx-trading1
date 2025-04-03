package com.okx.trading.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统工具类
 * 用于执行系统级别操作，如检查端口占用、终止进程等
 */
@Slf4j
public class SystemUtil {

    // Windows命令
    private static final String NETSTAT_COMMAND = "netstat -ano";
    private static final String KILL_PROCESS_COMMAND = "taskkill /F /PID %d";
    private static final Pattern PID_PATTERN = Pattern.compile("\\s+(\\d+)\\s*$");

    /**
     * 检查指定端口是否被占用
     *
     * @param port 要检查的端口号
     * @return 如果端口被占用返回true，否则返回false
     */
    public static boolean isPortInUse(int port) {
        List<Integer> pids = getProcessesUsingPort(port);
        return !pids.isEmpty();
    }

    /**
     * 获取占用指定端口的进程ID列表
     *
     * @param port 要检查的端口号
     * @return 占用该端口的进程ID列表
     */
    public static List<Integer> getProcessesUsingPort(int port) {
        List<Integer> pids = new ArrayList<>();
        String portStr = ":" + port;

        try {
            // 执行netstat命令
            log.info("执行命令: {}", NETSTAT_COMMAND);
            Process process = Runtime.getRuntime().exec(NETSTAT_COMMAND);

            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), Charset.forName("GBK")));
            String errorLine;
            boolean hasErrorOutput = false;
//            while ((errorLine = errorReader.readLine()) != null) {
//                hasErrorOutput = true;
//                log.warn("命令错误输出: {}", errorLine);
//            }
//
//            if (hasErrorOutput) {
//                log.warn("命令执行可能有问题，请检查错误输出");
//            }

            // 读取标准输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));

            String line;
            int lineCount = 0;
            log.debug("开始读取命令输出...");

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // 只输出前几行用于调试
                if (lineCount <= 5) {
                    log.debug("读取到行 #{}: {}", lineCount, line);
                } else if (lineCount == 6) {
                    log.debug("更多行省略...");
                }

                // 处理包含指定端口的行
                if (line.contains(portStr)) {
                    log.debug("发现包含端口 {} 的行: {}", port, line);

                    // 使用正则表达式提取PID
                    Matcher matcher = PID_PATTERN.matcher(line.trim());
                    if (matcher.find()) {
                        String pidStr = matcher.group(1).trim();
                        log.debug("匹配到PID: {}", pidStr);

                        try {
                            int pid = Integer.parseInt(pidStr);
                            if (pid > 0) { // 确保PID是有效的
                                pids.add(pid);
                                log.info("添加进程ID: {}", pid);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("无法解析进程ID: {}", pidStr);
                        }
                    } else {
                        log.debug("在行中未找到PID匹配: {}", line);
                    }
                }
            }

            log.info("共读取了 {} 行输出", lineCount);
            reader.close();
            errorReader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("命令执行返回非零退出码: {}", exitCode);
            }

            if (lineCount == 0) {
                log.warn("未读取到任何输出行，可能存在问题");
            }

            if (pids.isEmpty()) {
                log.info("未找到占用端口 {} 的进程", port);
            } else {
                log.info("找到 {} 个占用端口 {} 的进程: {}", pids.size(), port, pids);
            }
        } catch (IOException | InterruptedException e) {
            log.error("检查端口占用时出错: {}", e.getMessage(), e);
        }

        return pids;
    }

    /**
     * 终止占用指定端口的所有进程
     *
     * @param port 端口号
     * @return 成功终止的进程数量
     */
    public static int killProcessesUsingPort(int port) {
        List<Integer> pids = getProcessesUsingPort(port);
        int killedCount = 0;

        for (Integer pid : pids) {
            if (killProcess(pid)) {
                killedCount++;
            }
        }

        // 等待短暂时间确保进程已终止
        if (killedCount > 0) {
            try {
                log.info("等待进程完全终止...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return killedCount;
    }

    /**
     * 终止指定进程ID的进程
     *
     * @param pid 进程ID
     * @return 如果成功终止返回true，否则返回false
     */
    public static boolean killProcess(int pid) {
        String command = String.format(KILL_PROCESS_COMMAND, pid);

        try {
            log.info("执行命令: {}", command);
            Process process = Runtime.getRuntime().exec(command);

            // 读取命令执行结果
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("命令输出: {}", line);
            }

            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), Charset.forName("GBK")));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                log.warn("命令错误输出: {}", errorLine);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("成功终止进程: {}", pid);
                return true;
            } else {
                log.warn("终止进程失败, PID: {}, 退出码: {}", pid, exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("终止进程时出错, PID: {}: {}", pid, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查并终止指定端口的所有进程，用于在应用启动前清理
     *
     * @param port 要检查的端口号
     * @return 清理结果消息
     */
    public static String checkAndKillPort(int port) {
        log.info("检查端口 {} 是否被占用", port);
        List<Integer> pids = getProcessesUsingPort(port);

        if (pids.isEmpty()) {
            String message = "端口 " + port + " 未被占用";
            log.info(message);
            return message;
        }

        log.info("端口 {} 被 {} 个进程占用，尝试终止...", port, pids.size());
        int killedCount = 0;

        for (Integer pid : pids) {
            if (killProcess(pid)) {
                killedCount++;
            }
        }

        // 等待短暂时间确保进程已终止
        try {
            log.info("等待进程完全终止...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 再次检查端口状态
        List<Integer> remainingPids = getProcessesUsingPort(port);
        if (remainingPids.isEmpty()) {
            String message = "已成功终止所有占用端口 " + port + " 的进程: " + pids;
            log.info(message);
            return message;
        } else {
            String message = "已终止 " + killedCount + "/" + pids.size()
                    + " 个进程，但仍有 " + remainingPids.size()
                    + " 个进程占用端口 " + port + ": " + remainingPids;
            log.warn(message);
            return message;
        }
    }

    /**
     * 测试系统命令是否可用
     *
     * @return 测试结果
     */
    public static boolean testCommand() {
        try {
            log.info("测试执行系统命令...");
            Process process = Runtime.getRuntime().exec("ipconfig");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));

            int lineCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (lineCount <= 2) {
                    log.info("读取到测试输出行: {}", line);
                }
            }

            reader.close();
            int exitCode = process.waitFor();

            log.info("测试命令执行完成，读取了 {} 行，退出码: {}", lineCount, exitCode);
            return lineCount > 0 && exitCode == 0;

        } catch (Exception e) {
            log.error("测试命令执行失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
