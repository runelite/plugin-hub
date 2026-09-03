@echo off
setlocal EnableExtensions

rem Lightweight Windows launcher for this custom plugin package.
rem IntelliJ already downloads Gradle 8.14.3 from gradle-wrapper.properties.
rem Reuse that cached distribution so no gradle-wrapper.jar is required.

set "WRAPPER_ROOT=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.14.3-bin"
set "GRADLE_BAT="

if exist "%WRAPPER_ROOT%" (
  for /f "delims=" %%G in ('dir /b /s "%WRAPPER_ROOT%\gradle.bat" 2^>nul') do (
    if not defined GRADLE_BAT set "GRADLE_BAT=%%G"
  )
)

if not defined GRADLE_BAT (
  echo.
  echo Gradle 8.14.3 is not cached yet.
  echo.
  echo In IntelliJ, open the Gradle tool window and run the "help" task once.
  echo After it finishes successfully, run this command again:
  echo.
  echo     .\gradlew.bat run
  echo.
  exit /b 1
)

call "%GRADLE_BAT%" %*
exit /b %ERRORLEVEL%
