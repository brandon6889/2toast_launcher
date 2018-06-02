#!/bin/bash

user=$1
pass=$2

echo "Login:"
echo -n "    "
oldIFS="$IFS"
IFS=':'
set -- `curl -k -s -d "user=$user&pass=$pass&version=9999" https://auth.2toast.net/minecraft/login.php`
IFS="$oldIFS"
if [ $# -ne 4 ]
then
        echo "ERR: $@"
        exit 1
fi
sid=$4
user=$3
echo "$1:$2:$3:$4"
echo
hash=$RANDOM
echo "Join:"
echo -n "    "
curl -L "http://2toast.net/minecraft/joinserver.php?user=$user&sessionId=$sid&serverId=$hash"
echo
echo
echo "Check:"
echo -n "    "
curl -L "http://2toast.net/minecraft/checkserver.php?user=$user&serverId=$hash"
echo
