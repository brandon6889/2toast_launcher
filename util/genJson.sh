# Leaves a trailing comma on last array element, just delete that when done.

echo {; echo "  \"mods\": ["; for f in `find 1.4.7 -type f`; do echo "    {"; echo -n "      \"name\": \""; echo $f | awk -F/ '{ printf $2; }'; echo \",; echo "      \"size\": "$(ls -l $f | awk -F' ' '{printf $5; print ","}') ; echo "      \"hash\": \""$(sha1sum $f | awk -F' ' '{print $1}')\" ; echo "    },"; done; echo "  ]"; echo }
