@ECHO OFF
SETLOCAL


:BEGIN
CLS
COLOR 3F >nul 2>&1
SET MC_SYS32=%SYSTEMROOT%\SYSTEM32
REM Make batch directory the same as the directory it's being called from
REM For example, if "run as admin" the batch starting dir could be system32
CD "%~dp0" >nul 2>&1


:CHECKJAVA
ECHO INFO: Checking java installation...
ECHO.

REM If no Java is installed this line will catch it simply
java -d64 -version >nul 2>&1 || GOTO JAVAERROR

REM Look for Java 1.8 specifically
java -d64 -version 2>&1 | %MC_SYS32%\FIND.EXE "1.8"
ECHO.
IF %ERRORLEVEL% EQU 0 (
	ECHO INFO: Found 64-bit Java 1.8
	GOTO MAIN
) ELSE (
    GOTO JAVAERROR
)


:MAIN
java -d64 -jar serverstarter.jar
GOTO EOF


:JAVAERROR
COLOR CF
ECHO ERROR: Could not find 64-bit Java 1.8 installed or in PATH
PAUSE


:EOF