package javadecompiler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.exception.DecompilerException;
import javadecompiler.output.JavaFileWriter;

public class JavaDecompiler
{
    private static Properties properties;
    private static String className;
    private static boolean createByteCodeFlieFl = false;
    private static String byteCodeInstructionFile = "jvminstruction.xls";
    static
    {
        try
        {
            properties = new Properties();
            properties.load( new FileInputStream( "../config/config.props" ) );
            className = (String) properties.get( "input_class_name" );
            
            if ( className == null )
            {
                System.out.println( "Please provide Input file " );
                System.exit( 0 );
            }
            String createBytecodeFile = (String) properties.get( "create_byte_code_file" );
            createByteCodeFlieFl = createBytecodeFile != null && createBytecodeFile.equalsIgnoreCase( "true" );
            
            String javabytecodefile = (String) properties.get( "javabytecodefile" );
            
            if ( javabytecodefile != null )
            {
                byteCodeInstructionFile = javabytecodefile;
            }
            
        }
        catch ( Exception e )
        {
            System.out.println( "Unable to load config.props file" );
            System.exit( 0 );
        }

    }

    public static void main( String[] args ) throws IOException, DecompilerException
    {


        ByteCodeParser byteCodeParser = new ByteCodeParser();

        byteCodeParser.intialize( className, createByteCodeFlieFl, byteCodeInstructionFile );

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
        jfw.write( className );
        jfw.finalise();

    }
}
