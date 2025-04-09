@echo off
echo Building AgroYard App...
set ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools\bin;%ANDROID_HOME%\platform-tools
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

echo Using Android SDK at: %ANDROID_HOME%
echo =================================

call gradlew clean assembleDebug --stacktrace

if %ERRORLEVEL% EQU 0 (
  echo =================================
  echo Build completed successfully!
  echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
  echo =================================
  echo Build failed with error code: %ERRORLEVEL%
)

pause 