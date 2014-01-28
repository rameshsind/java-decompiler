package javadecompiler.output;

import java.io.FileWriter;
import java.io.IOException;

import javadecompiler.bytecode.decoder.BytecodeToJavaDecoder;
import javadecompiler.bytecode.parser.ByteCodeParser;

public class JavaFileWriter
{
    private static final String CLOSE_BRACE = "}";
    private static final String OPEN_BRACE = "{";
    private static final String NEWLINE = "\n";
    ByteCodeParser byteCodeParser;
    BytecodeToJavaDecoder bytecodeToJavaConvertor;
    StringBuilder output;
    FileWriter fw;

    public JavaFileWriter( ByteCodeParser byteCodeParser )
    {
        this.byteCodeParser = byteCodeParser;
        this.output = new StringBuilder();
    }

    public void intialize()
    {
        bytecodeToJavaConvertor = new BytecodeToJavaDecoder( byteCodeParser );
    }
    
    public void write( String inputFileName, StringBuilder javaOutput  )
    {

        String classString = bytecodeToJavaConvertor.getClassString();
        String interfaceString = bytecodeToJavaConvertor.getAllInterfaceDetails();
        String fieldsString = bytecodeToJavaConvertor.getAllFieldDetails();
        String methodsString = bytecodeToJavaConvertor.getAllMethodDetails();

            System.out.println( "\n\n\n\n\n\n\n" );
            if ( !"".equals( bytecodeToJavaConvertor.packageAndClassName[0] ) )
            {
                System.out.println( "package " + bytecodeToJavaConvertor.packageAndClassName[0] + ";" );
                javaOutput.append( "package " + bytecodeToJavaConvertor.packageAndClassName[0] + ";" );
            }
            
            javaOutput.append(  bytecodeToJavaConvertor.getImportItems() );
            javaOutput.append( NEWLINE );
            System.out.println( bytecodeToJavaConvertor.getImportItems() );
            javaOutput.append(  classString + interfaceString );
            javaOutput.append( NEWLINE );
            System.out.println( classString + interfaceString );
            javaOutput.append(  OPEN_BRACE );
            javaOutput.append( NEWLINE );
            System.out.println( OPEN_BRACE );
            javaOutput.append(  fieldsString );
            javaOutput.append( NEWLINE );
            System.out.println( fieldsString );
            javaOutput.append( methodsString );
            javaOutput.append( NEWLINE );
            System.out.println( methodsString );
            javaOutput.append( CLOSE_BRACE );
            javaOutput.append( NEWLINE );
            System.out.println( CLOSE_BRACE );
            
            
        
    }
    
    public void write( String inputFileName )
    {

        String classString = bytecodeToJavaConvertor.getClassString();
        String interfaceString = bytecodeToJavaConvertor.getAllInterfaceDetails();
        String fieldsString = bytecodeToJavaConvertor.getAllFieldDetails();
        String methodsString = bytecodeToJavaConvertor.getAllMethodDetails();

        try
        {
            fw = new FileWriter( inputFileName + ".java" );

            System.out.println( "\n\n\n\n\n\n\n" );
            if ( !"".equals( bytecodeToJavaConvertor.packageAndClassName[0] ) )
            {
                System.out.println( "package " + bytecodeToJavaConvertor.packageAndClassName[0] + ";" );
                fw.write( "package " + bytecodeToJavaConvertor.packageAndClassName[0] + ";" );
            }
            
            fw.write(  bytecodeToJavaConvertor.getImportItems() );
            fw.write( NEWLINE );
            System.out.println( bytecodeToJavaConvertor.getImportItems() );
            fw.write(  classString + interfaceString );
            fw.write( NEWLINE );
            System.out.println( classString + interfaceString );
            fw.write(  OPEN_BRACE );
            fw.write( NEWLINE );
            System.out.println( OPEN_BRACE );
            fw.write(  fieldsString );
            fw.write( NEWLINE );
            System.out.println( fieldsString );
            fw.write( methodsString );
            fw.write( NEWLINE );
            System.out.println( methodsString );
            fw.write( CLOSE_BRACE );
            fw.write( NEWLINE );
            System.out.println( CLOSE_BRACE );
            fw.flush();
            fw.close();
            
        }
        catch ( IOException e )
        {
            if(fw != null)
            {
                try
                {
                    fw.close();
                }
                catch ( IOException e1 )
                {
                    System.out.println("unable to close the file writer");
                }
            }
            e.printStackTrace();
        }
    }

    public void finalise()
    {
        // TODO Auto-generated method stub

    }

}
