rm -rf out dist
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
  --overwrite

zip -r SimEngine.zip release/SimEngine.app