package com.okx.trading.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemUtil工具类测试
 */
public class SystemUtilTest {

    private static final Logger log = LoggerFactory.getLogger(SystemUtilTest.class);
    
    // 测试端口，选择一个常见端口进行测试
    private static final int TEST_PORT = 8088;

    /**
     * 测试获取进程列表功能
     */
    @Test
    public void testGetProcessesUsingPort() {
        log.info("开始测试获取占用端口 {} 的进程", TEST_PORT);
        
        // 调用需要测试的方法
        List<Integer> pids = SystemUtil.getProcessesUsingPort(TEST_PORT);
        
        // 记录结果
        log.info("测试结果: 发现 {} 个占用端口 {} 的进程", pids.size(), TEST_PORT);
        
        // 我们不断言具体有多少个进程，因为这取决于实际运行环境
        // 但我们至少可以验证方法正常执行并返回一个列表
        assertNotNull(pids, "进程ID列表不应为null");
    }
    
    /**
     * 测试端口占用检查功能
     */
    @Test
    public void testIsPortInUse() {
        log.info("开始测试端口 {} 是否被占用", TEST_PORT);
        
        // 调用需要测试的方法
        boolean inUse = SystemUtil.isPortInUse(TEST_PORT);
        
        // 记录结果
        log.info("测试结果: 端口 {} {}", TEST_PORT, inUse ? "被占用" : "未被占用");
        
        // 不断言具体结果，只确保方法正常执行
    }
    
    /**
     * 测试使用特定命令获取命令输出
     * 这个测试用例专门验证reader.readLine()能够正确读取命令输出
     */
    @Test
    public void testCommandOutput() {
        log.info("开始测试命令输出读取");
        
        try {
            // 使用一个简单的命令，确保有输出
            String command = "ipconfig";
            Process process = Runtime.getRuntime().exec(command);
            
            // 创建读取器
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.Charset.forName("GBK")));
            
            // 读取输出
            String line;
            int lineCount = 0;
            StringBuilder output = new StringBuilder();
            
            log.info("开始读取命令 '{}' 的输出...", command);
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (lineCount <= 5) { // 只记录前5行，避免日志过多
                    output.append(line).append("\n");
                }
            }
            
            // 记录读取结果
            log.info("成功读取 {} 行输出", lineCount);
            log.info("前 {} 行输出内容:\n{}", Math.min(5, lineCount), output.toString());
            
            // 断言确实读取到了输出
            assertTrue(lineCount > 0, "应该至少读取到一行输出");
            
            reader.close();
            process.waitFor();
            
        } catch (Exception e) {
            log.error("测试过程中发生错误", e);
            fail("测试执行时发生异常: " + e.getMessage());
        }
    }
} 