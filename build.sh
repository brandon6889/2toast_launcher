# Requires LZMA utils from the following URL:
#  http://tukaani.org/lzma/
echo -n "Cleaning dist... "
ant clean 1>/dev/null
echo done.
ant
pushd . 1>/dev/null
if [ -d dist ]; then
cd dist
for jar in *.jar
do
  echo -n "Pack200'ing the dist jar... "
  mv $jar launcher.jar
  pack200 --no-gzip launcher.pack launcher.jar
  mv launcher.jar $jar
  echo done.
  
  echo -n "LZMA'ing the Pack200'd jar... "
  lzma -z launcher.pack
  echo done.
  
  break
done
fi
popd 1>/dev/null
