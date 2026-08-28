# MediaStore

Android queries `MediaStore.Audio.Media` with `IS_MUSIC != 0`, uses content URIs, and requests `READ_MEDIA_AUDIO` on Android 13+. Do not use broad filesystem permissions.
