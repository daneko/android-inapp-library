## Android Inapp Library

* this is Inapp Billing v3 wrap Library

** this software includes the work that is distributed in the Apache License 2.0 **

### add dependencies

```
repositories {
    maven { url 'https://daneko.github.io/m2repo/repository' }
}

dependencies {
    compile 'com.github.daneko:android-inapp-library:0.1.1'
}
```


### use library

* [RxJava](https://github.com/ReactiveX/RxJava) ([license apache license 2.0](https://github.com/ReactiveX/RxJava/blob/v1.0.16/LICENSE))
* [FunctionalJava](http://www.functionaljava.org/) ([license BSD 3](https://github.com/functionaljava/functionaljava/tree/v4.4#license))
* [slf4j](http://slf4j.org/) ([license MIT](http://slf4j.org/license.html))
* [kotlin stdlib](https://kotlinlang.org/api/latest/jvm/stdlib/index.html) ([license apache license 2.0](https://github.com/JetBrains/kotlin/blob/build-1.0.0-beta%2B1019/license/LICENSE.txt))

### use library in sample app

* [ButterKnife](http://jakewharton.github.io/butterknife/) ([license apache license 2.0](https://github.com/JakeWharton/butterknife/blob/master/LICENSE.txt))
* [logback android](http://tony19.github.io/logback-android/) (license EPL)
* [retrolambda](https://github.com/orfjackal/retrolambda) ([license apache license 2.0](https://github.com/orfjackal/retrolambda/blob/master/LICENSE.txt))
* [Lombok](http://projectlombok.org/) [(license MIT)](https://github.com/rzwitserloot/lombok/blob/master/LICENSE)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid) ([license apache license 2.0](https://github.com/ReactiveX/RxAndroid/blob/v1.0.0/LICENSE))


### for Android Studio

#### Plugin

* Lombok
* ButterKnife

### local.properties sample

```
sdk.dir=/usr/local/opt/android-sdk
java8_home=/Library/Java/JavaVirtualMachines/jdk1.8.0_74.jdk/Contents/Home
java7_home=/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home

## for sample app

public_key=copy from google developer console
release.storeFile=/path/to/your.keystore
release.storePass=store pass
release.alias=alias
release.aliasPass=alias pass
```

### try alpha apk

* [register alpha test & download apk](https://play.google.com/apps/testing/jp.daneko.example)

### License

(The MIT License)

Copyright (c) 2014- daneko

