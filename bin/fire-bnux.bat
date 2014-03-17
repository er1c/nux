@echo off
if "%OS%" == "Windows_NT" setlocal

rem ----------------------------------------------------------------------
rem Shell script to start demo program
rem ----------------------------------------------------------------------

rem %~dp0 is expanded pathname of the current script under NT
set MYDIR=%~dp0%
call "%MYDIR%fire-java.bat" nux.xom.tests.BinaryXMLConverter %*

if "%OS%"=="Windows_NT" endlocal
