# ASeeker

### 说明

`ASeeker`项目是我们在做虚拟化沙箱产品过程中的内部副产品。目的是为了快速适配`Android`系统。通过对源码扫描，找出满足条件的所有服务接口（`AIDL`）类和方法，提升适配效率，减少遗漏。

更详细的解读，请查看我们的博客文章： [ASeeker：源码捞针，AIDL接口扫描神器](https://www.iofomo.com/blog/aseeker) 。

如果您也喜欢 `ASeeker`，别忘了给我们点个星。

### 编译

```shell
# clean
$ mvn clean package
# pack
$ mvn package
```

### 运行

```shell
# 确保 asseker.jar 和 res 在同一目录
# aseeker [-options]
#   -p [SDK version code] [source code path]
$ java -jar aseeker.jar -p 33 /Users/abc/android_13.0_r13
```

### 感谢小伙伴们

![](img/thanks.png)

### 许可

This project is licensed under the terms of the `MIT` license. See the [LICENSE](LICENSE) file.

>   This project and all tools are open source under the MIT license, which means you have full access to the source code and can modify it to fit your own needs. 
