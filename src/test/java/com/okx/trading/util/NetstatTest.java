package com.okx.trading.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Netstat命令测试类
 * 用于验证netstat命令输出是否可以正确读取
 */
public class NetstatTest {

    /**
     * 测试入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 测试端口
        int port = 8088;
        
        System.out.println("开始测试Netstat命令...");
        
        // 执行netstat命令
        List<Integer> pids = getProcessesByNetstat(port);
        
        // 打印结果
        System.out.println("找到 " + pids.size() + " 个占用端口 " + port + " 的进程: " + pids);
        
        System.out.println("测试完成!");
    }
    
    /**
     * 使用netstat命令获取占用指定端口的进程ID
     *
     * @param port 端口号
     * @return 进程ID列表
     */
    private static List<Integer> getProcessesByNetstat(int port) {
        List<Integer> pids = new ArrayList<>();
        String portStr = ":" + port;
        String command = "netstat -ano";
        
        try {
            System.out.println("执行命令: " + command);
            Process process = Runtime.getRuntime().exec(command);
            
            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), Charset.forName("GBK")));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.out.println("错误输出: " + errorLine);
            }
            
            // 读取标准输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
            
            String line;
            int lineCount = 0;
            System.out.println("\n开始读取命令输出...");
            
            List<String> matchingLines = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // 只显示前5行
                if (lineCount <= 5) {
                    System.out.println("行 " + lineCount + ": " + line);
                } else if (lineCount == 6) {
                    System.out.println("...(更多行省略)...");
                }
                
                // 处理包含指定端口的行
                if (line.contains(portStr)) {
                    matchingLines.add(line);
                    System.out.println("\n找到包含端口 " + port + " 的行: " + line);
                    
                    // 使用正则表达式提取PID
                    Pattern pattern = Pattern.compile("\\s+(\\d+)\\s*$");
                    Matcher matcher = pattern.matcher(line.trim());
                    
                    if (matcher.find()) {
                        String pidStr = matcher.group(1).trim();
                        System.out.println("提取到PID: " + pidStr);
                        
                        try {
                            int pid = Integer.parseInt(pidStr);
                            if (pid > 0) {
                                pids.add(pid);
                                System.out.println("添加进程ID: " + pid);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("无法解析进程ID: " + pidStr);
                        }
                    } else {
                        System.out.println("未能从该行提取PID");
                    }
                }
            }
            
            System.out.println("\n共读取了 " + lineCount + " 行输出");
            System.out.println("找到 " + matchingLines.size() + " 行包含端口 " + port + " 的数据");
            
            reader.close();
            errorReader.close();
            process.waitFor();
            
        } catch (Exception e) {
            System.out.println("执行netstat命令时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return pids;
    }
} 