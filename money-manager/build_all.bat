@echo off
echo ===================================================
echo   Money Manager: Building JAR and EXE Installer
echo ===================================================

:: 1. Check if JDK is available
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in your system PATH.
    echo Please make sure JDK 21 or JDK 17 is installed and added to your environment variables.
    pause
    exit /b 1
)

:: 2. Build the JAR using Maven Wrapper
echo.
echo [1/3] Compiling and packaging Shaded Fat JAR...
call mvnw.cmd clean package -DskipTests

if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed! Please check the output above.
    pause
    exit /b 1
)

echo [SUCCESS] Shaded JAR successfully created in target\

:: 3. Prepare the jpackage input folder
echo.
echo [2/3] Preparing packaging folders...
if exist target\installer-input rmdir /s /q target\installer-input
mkdir target\installer-input

:: Copy the generated shaded jar to the installer folder (ignoring the "original-" jar)
for %%f in (target\money-manager-*.jar) do (
    echo %%~nxf | findstr /i "original-" >nul
    if errorlevel 1 (
        copy "%%f" "target\installer-input\money-manager.jar" >nul
    )
)

if %errorlevel% neq 0 (
    echo [ERROR] Could not copy the JAR file. Check if a JAR file exists in the target\ folder.
    pause
    exit /b 1
)

:: 4. Run jpackage to build both the EXE Installer and the direct Extracted App Image
echo.
echo [3/4] Building native EXE installer using jpackage...
if exist output rmdir /s /q output
mkdir output

where jpackage >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] 'jpackage' command was not found in your standard PATH.
    echo Attempting to locate 'jpackage' inside your JAVA_HOME...
    
    if "%JAVA_HOME%"=="" (
        echo [ERROR] JAVA_HOME environment variable is not defined and 'jpackage' is not in PATH.
        echo Please set JAVA_HOME to your JDK folder or add the JDK bin folder to your PATH.
        pause
        exit /b 1
    )
    
    if not exist "%JAVA_HOME%\bin\jpackage.exe" (
        echo [ERROR] 'jpackage.exe' was not found in "%JAVA_HOME%\bin".
        echo Please ensure you are using JDK 14 or higher. JDK 21 is highly recommended.
        pause
        exit /b 1
    )
    
    set JP_PATH="%JAVA_HOME%\bin\jpackage.exe"
) else (
    set JP_PATH=jpackage
)

echo Running: %JP_PATH% (Building MSI Installer)
if exist target\msi-output rmdir /s /q target\msi-output
mkdir target\msi-output

%JP_PATH% ^
  --type msi ^
  --name MoneyManager ^
  --input target/installer-input ^
  --main-jar money-manager.jar ^
  --main-class com.moneymanager.Launcher ^
  --dest target/msi-output ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed to generate the MSI installer.
    pause
    exit /b 1
)

echo.
echo [4/5] Building native EXE installer using jpackage...
%JP_PATH% ^
  --type exe ^
  --name MoneyManager ^
  --input target/installer-input ^
  --main-jar money-manager.jar ^
  --main-class com.moneymanager.Launcher ^
  --dest output ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed to generate the EXE installer.
    pause
    exit /b 1
)

echo.
echo [5/5] Building extracted application folder (app-image)...
if exist target\app-image rmdir /s /q target\app-image

%JP_PATH% ^
  --type app-image ^
  --name MoneyManager ^
  --input target/installer-input ^
  --main-jar money-manager.jar ^
  --main-class com.moneymanager.Launcher ^
  --dest target/app-image

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed to generate the extracted application folder.
    pause
    exit /b 1
)

echo Copying MSI installer to output...
copy target\msi-output\*.msi output\ >nul

echo Copying extracted application to output\MoneyManager...
if exist output\MoneyManager rmdir /s /q output\MoneyManager
xcopy target\app-image\MoneyManager output\MoneyManager /E /I /H /Y >nul

if %errorlevel% neq 0 (
    echo [WARNING] Could not copy the extracted folder to output\MoneyManager.
)

echo.
echo ===================================================
echo   Build Completed Successfully!
echo ===================================================
echo   - JAR File: target\ (e.g., money-manager-1.0.0.jar)
echo   - MSI Installer: output\MoneyManager-1.0.msi
echo   - EXE Installer: output\MoneyManager-1.0.exe
echo   - Extracted Folder: output\MoneyManager\
echo ===================================================
echo.
pause
