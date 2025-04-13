# Redis Repository 目录

此目录用于存放所有Redis存储库接口，需使用`@RedisHash`注解标记实体类，并且Repository接口需要继承`KeyValueRepository`接口。

## 使用方式

```java
// Redis实体类示例
@RedisHash("cache_key_prefix")
public class RedisEntity {
    @Id
    private String id;
    // 其他字段...
}

// Redis存储库接口示例
public interface RedisEntityRepository extends KeyValueRepository<RedisEntity, String> {
    // 自定义查询方法...
}
```

## 注意事项

1. Redis实体类必须使用`@RedisHash`注解
2. Redis存储库接口应该放在此目录下
3. JPA存储库接口应该放在上级目录 