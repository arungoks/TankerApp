> Task :app:connectedDebugAndroidTest
Test run failed to complete. Instrumentation run failed due to Process crashed.
Logcat of last crash: 
Process: com.arun.tankerapp, PID: 7880
java.lang.RuntimeException: Unable to instantiate application com.arun.tankerapp.TankerApplication package com.arun.tankerapp: java.lang.ClassNotFoundException: Didn't find class "com.arun.tankerapp.TankerApplication" on path: DexPathList[[zip file "/system/framework/android.test.runner.jar", zip file "/system/framework/android.test.mock.jar", zip file "/system/framework/android.test.base.jar", zip file "/data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/base.apk", zip file "/data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/base.apk"],nativeLibraryDirectories=[/data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/lib/x86_64, /data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/lib/x86_64, /data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/base.apk!/lib/x86_64, /data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/base.apk!/lib/x86_64, /system/lib64, /system_ext/lib64]]
	at android.app.LoadedApk.makeApplicationInner(LoadedApk.java:1478)
	at android.app.LoadedApk.makeApplicationInner(LoadedApk.java:1403)
	at android.app.ActivityThread.handleBindApplication(ActivityThread.java:7464)
	at android.app.ActivityThread.-$$Nest$mhandleBindApplication(Unknown Source:0)
	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2416)
	at android.os.Handler.dispatchMessage(Handler.java:107)
	at android.os.Looper.loopOnce(Looper.java:232)
	at android.os.Looper.loop(Looper.java:317)
	at android.app.ActivityThread.main(ActivityThread.java:8705)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:580)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:886)
Caused by: java.lang.ClassNotFoundException: Didn't find class "com.arun.tankerapp.TankerApplication" on path: DexPathList[[zip file "/system/framework/android.test.runner.jar", zip file "/system/framework/android.test.mock.jar", zip file "/system/framework/android.test.base.jar", zip file "/data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/base.apk", zip file "/data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/base.apk"],nativeLibraryDirectories=[/data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/lib/x86_64, /data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/lib/x86_64, /data/app/~~mkgOAicmY6Xh_-d_QyMbxQ==/com.arun.tankerapp.test-ZCdP7AUWMCvwgpprmfL1XQ==/base.apk!/lib/x86_64, /data/app/~~OtZ5Fp6NapeYYNvht0JPsg==/com.arun.tankerapp-sbbSfbCDo3ph0Gp91XeDhw==/base.apk!/lib/x86_64, /system/lib64, /system_ext/lib64]]
	at dalvik.system.BaseDexClassLoader.findClass(BaseDexClassLoader.java:259)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:637)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:573)
	at android.app.AppComponentFactory.instantiateApplication(AppComponentFactory.java:76)
	at androidx.core.app.CoreComponentFactory.instantiateApplication(CoreComponentFactory.java:51)
	at android.app.Instrumentation.newApplication(Instrumentation.java:1352)
	at androidx.test.runner.MonitoringInstrumentation.newApplication(MonitoringInstrumentation.java:156)
	at androidx.test.runner.AndroidJUnitRunner.newApplication(AndroidJUnitRunner.java:300)
	at android.app.LoadedApk.makeApplicationInner(LoadedApk.java:1471)
	... 11 more


> Task :app:connectedDebugAndroidTest FAILED

[Incubating] Problems report is available at: file:///C:/Users/Arun/Documents/Projects/BMAD/TankerApp/TankerApp/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:connectedDebugAndroidTest'.
> There were failing tests. See the report at: file:///C:/Users/Arun/Documents/Projects/BMAD/TankerApp/TankerApp/app/build/reports/androidTests/connected/debug/index.html

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to generate a Build Scan (Powered by Develocity).
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.1.0/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 1m 51s