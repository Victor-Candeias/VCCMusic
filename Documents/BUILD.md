# Compilar e executar

## Requisitos

- JDK fornecido pelo Android Studio disponível através de `JAVA_HOME`.
- Android SDK API 37 e Build-Tools 36.0.0.
- Emulador ou dispositivo com API 26 ou superior.

## Comandos

No PowerShell, a partir da raiz do repositório:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleRelease
```

Para instalar o build debug no dispositivo ativo:

```powershell
.\gradlew.bat installDebug
```

Para executar os testes Compose no dispositivo ativo:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

O build release desta fase não tem assinatura de produção. Chaves e credenciais nunca devem ser guardadas no repositório.
