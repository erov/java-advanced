#!/bin/bash

kgeorgiyAtrifacts="../../java-advanced-2022/artifacts/"
kgeorgiyLibs="../../java-advanced-2022/lib/"
javaSolutions="../java-solutions/"
implementorPath="info/kgeorgiy/ja/erov/implementor/"
manifest=$javaSolutions$implementorPath"MANIFEST.MF"
jarName="implementor.jar"

mkdir out/
javac -p $kgeorgiyAtrifacts":"$kgeorgiyLibs -d out/ $(find $javaSolutions -name "*.java")
jar -c -f $jarName -m $manifest -p $kgeorgiyAtrifacts":"$kgeorgiyLibs -C out/ $implementorPath
rm -rf out/
echo $jarName" was created"