find . -name "*.class" -delete
mkdir -p out/assets
cp -R src/assets/* out/assets/
mkdir -p out/sound
cp -R src/sound/* out/sound/

javac -d out -sourcepath src src/main/GamePanel.java
java -cp out main.GamePanel
