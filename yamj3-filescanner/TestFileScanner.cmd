@echo off
set TESTDIR=%CD%\testdir
cd "%CD%\target\yamj3-filescanner-3.0-SNAPSHOT-bin"

call filescanner -d "%TESTDIR%" -w false
pause