@echo off
set TESTDIR=%CD%
cd "%CD%\target\yamj3-jetty-3.0-SNAPSHOT-bin"

del lib\*.war
copy ..\..\..\yamj3-core\target\*.war lib
call jetty
pause