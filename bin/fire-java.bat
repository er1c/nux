@echo off
if "%OS%" == "Windows_NT" setlocal

rem ---------------------------------------------------------------------------
rem Shell script to start a java program with the right classpath settings.
rem Example invocation: 
rem
rem 	fire-java.bat nux.xom.tests.XQueryCommand arg1 arg2 .. argN
rem
rem whoschek@lbl.gov
rem ---------------------------------------------------------------------------

rem %~dp0 is expanded pathname of the current script under NT
set MYDIR=%~dp0%

call "%MYDIR%util-find-jdk.bat"

set FIRE_HOME=%MYDIR%..
set FIRE_LIB=%FIRE_HOME%\lib

rem echo now detecting CLASSPATH...
set CLASSPATH=%FIRE_LIB%\..\build\classes;
set LOCALCLASSPATH=%CLASSPATH%
for %%i in (%FIRE_LIB%\*.jar) do call "%MYDIR%util-lcp.bat" %%i
rem for %%i in (%FIRE_LIB%\*.jar) do set CLASSPATH=%CLASSPATH%;%%i
set CLASSPATH=%LOCALCLASSPATH%
rem echo CLASSPATH is %CLASSPATH%
set CLASSPATH=%CLASSPATH%;%FIRE_HOME%\lib-for-build\stax-api-1.0.jar;
set CLASSPATH=%CLASSPATH%;%FIRE_HOME%\lib-for-build\junit.jar;

set opts=%JAVA_OPTS%
rem set opts=-Dlog4j.configuration=file:///%MYDIR%../log4j.properties
rem set opts=%opts% -Dlog4j.debug
rem echo opts are %opts%

call "%JAVA_HOME%\bin\java.exe" %opts% -cp %CLASSPATH% %*

if "%OS%"=="Windows_NT" endlocal

:end
