rem ----------------------------------------------------------------------
rem jdksetup script (to be sourced from some other .bat script)
rem
rem Searches for `java' in "standard" locations, then sets and returns ${JAVA_HOME}
rem ${JAVA_HOME}  --> path to the JDK installation
rem ----------------------------------------------------------------------

rem %~dp0 is expanded pathname of the current script under NT
set MYDIR=%~dp0%


rem echo now detecting JAVA_HOME...
if not "%JAVA_HOME%" == "" goto gotJavaHome

call "%MYDIR%util-find-jdk-path" java.exe
set JAVA_HOME=%PATH_TO_PROGRAM%..

set try=c:\java\jdk\sun-1.5.0
rem set try=e:\java\jdk\sun-1.5.0
if exist "%try%\bin\java.exe" set JAVA_HOME=%try%

:gotJavaHome
if exist "%JAVA_HOME%\bin\java.exe" goto okJavaHome
echo The JAVA_HOME or PATH environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okJavaHome

:end
rem echo JAVA_HOME is %JAVA_HOME%

