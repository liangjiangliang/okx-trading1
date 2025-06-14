-- 查看strategy_conversation表结构
DESC strategy_conversation;

-- 查看最新的对话记录
SELECT 
    id,
    strategy_id,
    conversation_type,
    SUBSTRING(user_input, 1, 50) as user_input_preview,
    SUBSTRING(compile_error, 1, 100) as compile_error_preview,
    create_time
FROM strategy_conversation 
ORDER BY create_time DESC 
LIMIT 10;

-- 查看有编译错误的记录
SELECT 
    id,
    strategy_id,
    conversation_type,
    compile_error,
    create_time
FROM strategy_conversation 
WHERE compile_error IS NOT NULL
ORDER BY create_time DESC;