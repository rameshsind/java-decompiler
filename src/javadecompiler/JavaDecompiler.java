package javadecompiler;

import java.io.IOException;

import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.exception.DecompilerException;
import javadecompiler.output.JavaFileWriter;

public class JavaDecompiler
{

    private boolean createByteCodeFlieFl = false;
    
    public void decompile( String className, StringBuilder[] output ) throws IOException, DecompilerException
    {

        ByteCodeParser byteCodeParser = new ByteCodeParser( output[0] );

        byteCodeParser.intialize( className );

        byteCodeParser.extractMagicNo();

        byteCodeParser.extractMajMinNo();

        byteCodeParser.extractConstantPool();

        byteCodeParser.extractAccessFlag();

        byteCodeParser.extractThisClass();

        byteCodeParser.extractSuperClass();

        byteCodeParser.extractInterfaces();

        byteCodeParser.extractFields();

        byteCodeParser.extractMethods();

        byteCodeParser.extractAttributes( "" );

        byteCodeParser.finalise();

        JavaFileWriter jfw = new JavaFileWriter( byteCodeParser );
        jfw.intialize();
        jfw.write( className, output[1] );
        jfw.finalise();

    }
}
