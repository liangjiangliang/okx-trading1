package com.okx.trading.util;

import java.util.List;

/**
 * 端口检查测试类
 * 用于直接运行验证端口检查功能
 */
public class PortCheckTester {

    /**
     * 测试入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("开始测试端口检查功能...");
        
        // 测试端口
        int port = 8088;
        
        // 测试获取占用指定端口的进程
        System.out.println("\n===== 测试获取占用端口 " + port + " 的进程 =====");
        List<Integer> pids = SystemUtil.getProcessesUsingPort(port);
        System.out.println("发现 " + pids.size() + " 个占用端口 " + port + " 的进程: " + pids);
        
        // 测试端口是否被占用
        System.out.println("\n===== 测试端口 " + port + " 是否被占用 =====");
        boolean inUse = SystemUtil.isPortInUse(port);
        System.out.println("端口 " + port + " " + (inUse ? "被占用" : "未被占用"));
        
        // 如果端口被占用，测试清理功能
        if (inUse) {
            System.out.println("\n===== 测试清理占用端口 " + port + " 的进程 =====");
            String result = SystemUtil.checkAndKillPort(port);
            System.out.println("清理结果: " + result);
            
            // 再次检查端口状态
            System.out.println("\n===== 清理后再次检查端口 " + port + " 状态 =====");
            boolean stillInUse = SystemUtil.isPortInUse(port);
            System.out.println("端口 " + port + " " + (stillInUse ? "仍然被占用" : "已经释放"));
        }
        
        System.out.println("\n测试完成!");
    }
} 