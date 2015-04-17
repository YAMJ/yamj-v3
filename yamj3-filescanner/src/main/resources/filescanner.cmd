@echo off
REM ${project.name}
REM ${project.description}
REM Version: ${project.version}
REM Git-SHA: ${git.commit.id}
cls
java -classpath .;./lib/* org.yamj.filescanner.FileScanner %*
