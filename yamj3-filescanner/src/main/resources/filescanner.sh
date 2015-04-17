#!/bin/sh
# ${project.name}
# ${project.description}
# Version: ${project.version}
# Git-SHA: ${git.commit.id}
clear
java -classpath .:./lib/* org.yamj.filescanner.FileScanner $@
