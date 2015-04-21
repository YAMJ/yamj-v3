#!/bin/sh
# ${project.name}
# ${project.description}
# Version: ${project.version}
# Git-SHA: ${git.commit.id}
#
# Run the jetty process as a daemon (not connected to the terminal)
setsid ./jetty.sh >/dev/null 2>&1< /dev/null &
