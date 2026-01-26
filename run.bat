@echo off
rem Use UTF-8 code page for proper Vietnamese output/compilation
chcp 65001 >nul
rem Compile Java sources with UTF-8 encoding
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\com\pos\Main.java src\com\pos\ui\*.java src\com\pos\db\*.java src\com\pos\util\*.java src\com\pos\dao\*.java src\com\pos\model\*.java src\com\pos\service\*.java src\com\pos\*.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
echo Running application...
rem If you placed MySQL Connector/J in lib (e.g. lib\mysql-connector-java-8.0.xx.jar), include it automatically via lib/*
if exist lib\\mysql-connector-j-9.5.0.jar (
    java -cp "bin;lib\\mysql-connector-j-9.5.0.jar" com.pos.Main
) else if exist lib (
    java -cp "bin;lib\\*" com.pos.Main
) else (
    java -cp bin com.pos.Main
)
pause
