#!/bin/bash

# 替换javax.persistence为jakarta.persistence
find src/main/java -name "*.java" -type f -exec sed -i 's/javax\.persistence/jakarta.persistence/g' {} \;

# 替换javax.annotation为jakarta.annotation
find src/main/java -name "*.java" -type f -exec sed -i 's/javax\.annotation/jakarta.annotation/g' {} \;

# 替换javax.validation为jakarta.validation
find src/main/java -name "*.java" -type f -exec sed -i 's/javax\.validation/jakarta.validation/g' {} \;

# 替换javax.mail为jakarta.mail
find src/main/java -name "*.java" -type f -exec sed -i 's/javax\.mail/jakarta.mail/g' {} \;

# 替换io.swagger.annotations为io.swagger.v3.oas.annotations
find src/main/java -name "*.java" -type f -exec sed -i 's/io\.swagger\.annotations/io.swagger.v3.oas.annotations/g' {} \;

# 替换@ApiModel为@Schema
find src/main/java -name "*.java" -type f -exec sed -i 's/@ApiModel/@Schema/g' {} \;

# 替换@ApiModelProperty为@Schema
find src/main/java -name "*.java" -type f -exec sed -i 's/@ApiModelProperty/@Schema/g' {} \;

# 替换@Api为@Tag
find src/main/java -name "*.java" -type f -exec sed -i 's/@Api(/@Tag(/g' {} \;

# 替换@ApiOperation为@Operation
find src/main/java -name "*.java" -type f -exec sed -i 's/@ApiOperation/@Operation/g' {} \;

# 替换@ApiParam为@Parameter
find src/main/java -name "*.java" -type f -exec sed -i 's/@ApiParam/@Parameter/g' {} \;

echo "所有导入已更新完成" 