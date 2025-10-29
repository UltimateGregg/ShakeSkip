@ECHO OFF
@REM ##########################################################################
@REM #
@REM #  Gradle startup script for Windows
@REM #
@REM ##########################################################################

@REM Set local scope for the variables with windows NT shell
IF "%OS%"=="Windows_NT" SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%

@REM Resolve any "." and ".." in APP_HOME to make it shorter.
FOR %%i IN ("%APP_HOME%") DO SET APP_HOME=%%~fi

@REM Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
SET DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@REM Find java.exe
IF DEFINED JAVA_HOME GOTO findJavaFromJavaHome

SET JAVA_EXE=java.exe
"%JAVA_EXE%" -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO execute

ECHO.
ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
ECHO.
ECHO Please set the JAVA_HOME variable in your environment to match the
ECHO location of your Java installation.

GOTO fail

:findJavaFromJavaHome
SET JAVA_HOME=%JAVA_HOME:"=%
SET JAVA_EXE=%JAVA_HOME%\bin\java.exe

IF EXIST "%JAVA_EXE%" GOTO execute

ECHO.
ECHO ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
ECHO.
ECHO Please set the JAVA_HOME variable in your environment to match the
ECHO location of your Java installation.

GOTO fail

:execute
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

SET APP_ARGS=

:findAppArgs
IF "%1"=="" GOTO executeJava
SET APP_ARGS=%APP_ARGS% %1
SHIFT
GOTO findAppArgs

:executeJava
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %APP_ARGS%

GOTO end

:fail
SET EXIT_CODE=%ERRORLEVEL%
IF %EXIT_CODE% EQU 0 SET EXIT_CODE=1
EXIT /B %EXIT_CODE%

:end
IF "%OS%"=="Windows_NT" ENDLOCAL
EXIT /B %ERRORLEVEL%
