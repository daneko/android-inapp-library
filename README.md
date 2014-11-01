### Inapp Billing v3 wrap Library

* SDKに付属するサンプルをベースにRxJava使って再実装してみた
* だれか「こう書くんだよ！」みたいなツッコミください
* 動作未保証
  * ていうかまだ全部は確認してない (できてない)

** this software includes the work that is distributed in the Apache License 2.0 **

### add dependencies

```
repositories {
    maven { url 'https://daneko.github.io/m2repo/repository' }
}

dependencies {
    compile 'com.github.daneko:android-inapp-library:0.0.1-SNAPSHOT'
}
```


### use library

* [Lombok](http://projectlombok.org/) [(license MIT)](https://github.com/rzwitserloot/lombok/blob/master/LICENSE)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid) ([license apache license 2.0](https://github.com/ReactiveX/RxAndroid/blob/0.x/LICENSE))
* [retrolambda](https://github.com/orfjackal/retrolambda) ([license apache license 2.0](https://github.com/orfjackal/retrolambda/blob/master/LICENSE.txt))
* [FunctionalJava](http://www.functionaljava.org/) ([license BSD 3](https://github.com/functionaljava/functionaljava#license))
* [JSR305](https://code.google.com/p/jsr-305/) ([license BSD 3](http://opensource.org/licenses/BSD-3-Clause))
* [slf4j](http://slf4j.org/) ([license MIT](http://slf4j.org/license.html))

### use library in sample app

* [ButterKnife](http://jakewharton.github.io/butterknife/) ([license apache license 2.0](https://github.com/JakeWharton/butterknife/blob/master/LICENSE.txt))
* [logback android](http://tony19.github.io/logback-android/) (license EPL)

### for Android Studio

#### Plugin

* Lombok
* ButterKnife

### local.properties sample

```
sdk.dir=/usr/local/opt/android-sdk
java8_home=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home
java7_home=/Library/Java/JavaVirtualMachines/jdk1.7.0_67.jdk/Contents/Home

## for sample app

public_key=copy from google developer console
release.storeFile=/path/to/your.keystore
release.storePass=store pass
release.alias=alias
release.aliasPass=alias pass
```

### try alpha apk

* join [google group](https://groups.google.com/forum/#!forum/daneko-testapp)
* and [download apk](https://play.google.com/apps/testing/jp.daneko.example)

### License

(The MIT License)

Copyright (c) 2014 daneko

