ZIPALIGN=$(ls ~/Library/Android/sdk/build-tools/*/zipalign | grep -v '21.')
rm ~/Projects/dancedeets-newdroid/app/build/outputs/apk/app-release-aligned.apk
$ZIPALIGN 4 ~/Projects/dancedeets-newdroid/app/build/outputs/apk/app-release-unaligned.apk ~/Projects/dancedeets-newdroid/app/build/outputs/apk/app-release-aligned.apk
