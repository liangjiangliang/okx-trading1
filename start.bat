@echo off
echo Starting OKX Trading Application...
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
java %JAVA_OPTS% -jar target/okx-trading-0.0.1-SNAPSHOT.jar
echo Application started
pause
