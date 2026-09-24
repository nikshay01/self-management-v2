# Samsung Health Data SDK Library Directory

Place the Samsung Health Data SDK `.aar` file here.

## File Required
`samsung-health-data-api-*.aar` (e.g., `samsung-health-data-api-1.5.0.aar` or current version)

## Download Instructions
1. Go to the [Samsung Developer Health portal](https://developer.samsung.com/health)
2. Sign in with your Samsung Developer account
3. Navigate to **Samsung Health Data SDK** -> **Downloads**
4. Download the SDK package and extract the `.aar` file
5. Copy the `.aar` file to this folder (`app/libs/`)

Once placed here, Gradle automatically picks up all `.aar` files in this directory via:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
```
