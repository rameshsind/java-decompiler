@echo "Creating classes & report  directories"

mkdir classes

cd src

@echo "Setting classpath"

set CLASSPATH=..\lib\jxl.jar;.;


@echo "Compiling source files"

javac -d ..\classes javadecompiler\*.java

cd ..\classes

java javadecompiler.JavaDecompiler

cd ..








