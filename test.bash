#!/bin/bash
# Compile le jeu et les tests dans out-test/, puis lance chaque test.
set -e
rm -rf out-test
mkdir -p out-test
cp -R src/assets src/sound out-test/
javac -d out-test -sourcepath src:test $(find test -name "*Test.java")
for test in $(cd test && find . -name "*Test.java" | sed 's|^\./||; s|\.java$||; s|/|.|g'); do
    java -Djava.awt.headless=true -cp out-test "$test"
done
