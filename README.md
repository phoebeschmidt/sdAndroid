##**To Build**##

1. `brew install android-ndk`

2. clone this project and follow "How To" instructions in readme:
https://github.com/jaroslavas/Gstreamer-Android-example

* If you don't know your sdk path, you can find it in:
    AndroidStudio Preferences--> Appearance and Behavior-->System Settings-->SDK

3. Install any dependencies or missing things Android Studio asks you to

4. Try to save and gradle sync

###Troubleshooting Errors###

 
**1.** 
```
Error:(19, 0) Error: NDK integration is deprecated in the current plugin.  
Consider trying the new experimental plugin.  
For details, see http://tools.android.com/tech-docs/new-build-system/gradle-experimental.
```
+ Open gradle.properties or create one if necessary
+ inside gradle.properties set `android.useDeprecatedNdk=true` to continue using the current NDK integration
+ gradle sync. This error should have disappeared.

**2.** 
```
Error:(58, 0) No signature of method: 
com.android.build.gradle.AppPlugin.getNdkFolder() is applicable 
for argument types: () values: []
```

This is happening because the version of gradle from the original tutorial is old. We need to update some simple syntax used to fetch plugins.
To fix:

+ in Build.gradle (Module:app), modify the ndkBuild task to look like this:

```java
task ndkBuild(type: Exec, description: 'Compile JNI source via NDK') {
    def ndkDir = plugins.getPlugin('com.android.application').sdkHandler.getNdkFolder()
    println(project.plugins.getPlugin('com.android.application').sdkHandler.getNdkFolder())
```


Currently this is a clone of https://github.com/jaroslavas/Gstreamer-Android-example. We are working on getting it up and running and modifying the functionality.