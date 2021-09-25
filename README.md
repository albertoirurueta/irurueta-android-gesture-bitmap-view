# irurueta-android-gesture-bitmap-view
A touch gesture capable Android view to display bitmaps and perform scaling, translation and 
rotation on the bitmap

[![Build Status](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/main.yml/badge.svg)](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=alert_status)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

To execute all unit tests, execute:
```
gradlew test
```

To execute all instrumentation tests, first make sure you have connected a device or emulator and 
execute:
```
gradlew connectedAndroidTest
```

If you only want to run tests of a specific module, execute:
```
gradlew lib:test
```

or

```
gradlew lib:connectedAndroidTest
```

where "lib" is the name of a module in the project

To generate documentation, run:
```
gradlew dokkaHtml
```

To execute sonarqube, run:
```
gradlew sonarqube
```

