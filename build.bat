@echo "Creating classes & report  directories"

mkdir classes

cd src

@echo "Setting classpath"

set CLASSPATH=..\lib\jxl.jar;.;


@echo "Compiling source files"

javac -d ..\classes javadecompiler\gui\*.java

copy javadecompiler\bytecode\parser\opcode\jvminstruction.xls ..\classes\javadecompiler\bytecode\parser\opcode\


cd ..

copy -r lib classes\


jar -cvfm javadecompiler.jar manifest.mf  -C classes/ .








