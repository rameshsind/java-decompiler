package javadecompiler.bytecode.decoder;

import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.constantpool.type.ConstantClass;
import javadecompiler.constantpool.type.ConstantUtf8;

public class InterfaceJavaDecoder
{
    private ByteCodeParser byteCodeParser;
    private BytecodeToJavaDecoder bytecodeToJavaDecoder;   
    public InterfaceJavaDecoder( ByteCodeParser byteCodeParser, BytecodeToJavaDecoder bytecodeToJavaDecoder )
    {
        this.byteCodeParser = byteCodeParser;
        this.bytecodeToJavaDecoder = bytecodeToJavaDecoder;
    }

    public String decode()
    {
        StringBuilder interfaceData = new StringBuilder();
        int[] interfaceDetails = byteCodeParser.getInterfaceDetails();
        if ( interfaceDetails == null )
            return "";
        interfaceData.append( " implements " );
        for ( int interfaceDetail : interfaceDetails )
        {
            String name = "";
            ConstantClass cc = byteCodeParser.getConstantClassMap().get( interfaceDetail );
            ConstantUtf8 cUtf8 = byteCodeParser.getConstantUtf8Map().get( cc.nameIndex );

            int index = cUtf8.utf8String.lastIndexOf( "/" );

            if ( index == -1 )
            {
                name = cUtf8.utf8String;
            }
            else
            {
                name = bytecodeToJavaDecoder.extractClassAndPackage( cUtf8.utf8String );
            }
            if ( !interfaceData.toString().equals( " implements " ) )
            {
                interfaceData.append( ", " );
            }
            interfaceData.append( name );
        }
        return interfaceData.toString();
    }

}
