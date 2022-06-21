#!/bin/bash

kgeorgiyAtrifacts="../../java-advanced-2022/artifacts/"
kgeorgiyLibs="../../java-advanced-2022/lib/"
kgeorgiyModules="../../java-advanced-2022/modules/"
kgeorgiyImplementor=$kgeorgiyModules"info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"
javaSolutions="../java-solutions/"
implementor="info.kgeorgiy.ja.erov/info.kgeorgiy.ja.erov.implementor"
javadocDir="../javadoc/"

module="info.kgeorgiy.ja.erov/"
cp -r $javaSolutions $module
javadoc -quiet -p $kgeorgiyLibs":"$kgeorgiyAtrifacts --module-source-path .":"$kgeorgiyModules -private -d $javadocDir $implementor $kgeorgiyImplementor"Impler.java" $kgeorgiyImplementor"JarImpler.java" $kgeorgiyImplementor"ImplerException.java"
rm -rf $module
echo $implementor" documentation was created in "$javadocDir