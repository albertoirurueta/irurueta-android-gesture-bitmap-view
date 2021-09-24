# irurueta-android-gesture-bitmap-view
A touch gesture capable Android view to display bitmaps and perform scaling, translation and 
rotation on the bitmap

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

