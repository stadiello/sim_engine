rm -rf out dist release
rm -f manifest.txt sources.txt

find . -name "*.class" -delete
mkdir -p out/assets
cp -R src/assets/* out/assets/
mkdir -p out/sound
cp -R src/sound/* out/sound/

javac -d out -sourcepath src src/main/GamePanel.java

mkdir -p out dist
find src -name "*.java" > sources.txt
javac -d out -sourcepath src @sources.txt
cp -R src/assets out/assets
echo "Main-Class: main.GamePanel" > manifest.txt
jar cfm dist/sim_engine.jar manifest.txt -C out .

jpackage \
  --type app-image \
  --name SimEngine \
  --input dist \
  --main-jar sim_engine.jar \
  --main-class main.GamePanel \
  --dest release \
  # --overwrite

zip -r SimEngine.zip release/SimEngine.app