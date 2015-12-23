ZIPALIGN=$(ls ~/Library/Android/sdk/build-tools/*/zipalign | grep -v '23.')
BASEDIR=$(dirname $0)
rm $BASEDIR/app/app-release-aligned.apk
$ZIPALIGN 4 $BASEDIR/app/app-release.apk $BASEDIR/app/app-release-aligned.apk
