# Leaves a trailing comma on last array element, just delete that when done.

(echo {; echo "  \"mods\": ["; for f in `find 1.10.2 -type f`; do echo "    {"; echo -n "      \"name\": \""; echo $f | awk -F/ '{ OFS="/"; $1=""; printf substr($0,2); }'; echo \",; echo "      \"size\": "$(ls -l $f | awk -F' ' '{printf $5; print ","}') ; echo "      \"hash\": \""$(sha1sum $f | awk -F' ' '{print $1}')\" ; echo "    },"; done; echo "  ]"; echo })>1.10.2.json
