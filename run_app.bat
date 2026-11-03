@echo off
set "JAVA_HOME=C:\Users\Administrator\.gemini\antigravity\tools\jdk-21.0.5+11"
set "PATH=%JAVA_HOME%\bin;C:\Users\Administrator\.gemini\antigravity\tools\apache-maven-3.9.6\bin;%PATH%"
cd /d "C:\Users\Administrator\.gemini\antigravity\scratch\VieShift_v2"

echo ==========================================
echo    KIEM TRA VA KHOI DONG POSTGRESQL
echo ==========================================
sc query postgresql_vieshift | find "RUNNING" >nul
if %ERRORLEVEL% neq 0 (
    echo Dang khoi chay PostgreSQL...
    net start postgresql_vieshift 2>nul
    if %ERRORLEVEL% neq 0 (
        "C:\Users\Administrator\.gemini\antigravity\tools\pgsql\bin\pg_ctl.exe" -D "C:\Users\Administrator\.gemini\antigravity\tools\pgsql\data" -l "C:\Users\Administrator\.gemini\antigravity\tools\pgsql\logfile.log" start
    )
)

echo ==========================================
echo    KHOI DONG UNG DUNG VIESHIFT
echo    Truy cap: http://localhost:8080
echo ==========================================
mvn spring-boot:run
pause