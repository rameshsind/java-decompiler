package javadecompiler.bytecode.decoder;

import javadecompiler.bytecode.entities.FieldDetail;
import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.constantpool.type.ConstantDouble;
import javadecompiler.constantpool.type.ConstantFloat;
import javadecompiler.constantpool.type.ConstantInteger;
import javadecompiler.constantpool.type.ConstantString;

public class FieldJavaDecoder
{
    private ByteCodeParser byteCodeParser;
    private BytecodeToJavaDecoder bytecodeToJavaDecoder;
    public FieldJavaDecoder( ByteCodeParser byteCodeParser, BytecodeToJavaDecoder bytecodeToJavaDecoder )
    {
        this.byteCodeParser = byteCodeParser;
        this.bytecodeToJavaDecoder = bytecodeToJavaDecoder;
    }

    public String decode()
    {
        StringBuilder fieldsData = new StringBuilder();
        FieldDetail[] fieldDetails = byteCodeParser.getFieldDetails();
        if ( fieldDetails == null )
            return "";
        for ( FieldDetail fieldDetail : fieldDetails )
        {
            fieldsData.append( DecodeHelper.FORMATTED_SPACE );
            fieldsData.append( DecodeHelper.getAccessSpecifier( fieldDetail.accessFlag, false ) );
            /*
             if ( fieldDetail.fieldSignatureIndex != -1 )
             {
                 fieldsData.append( byteCodeParser.getConstantUtf8Map().get( fieldDetail.fieldSignatureIndex ).utf8String );
             }
             else
             {
                 fieldsData.append( getFormatedFieldType( byteCodeParser.getConstantUtf8Map().get( fieldDetail.descriptorIndex ).utf8String ) );
             }
             */
            String fieldType = byteCodeParser.getConstantUtf8Map().get( fieldDetail.descriptorIndex ).utf8String;
            fieldsData.append( bytecodeToJavaDecoder.getFormatedFieldType( fieldType ) );
            fieldsData.append( " " );
            fieldsData.append( byteCodeParser.getConstantUtf8Map().get( fieldDetail.nameIndex ).utf8String );
            constantValue: if ( fieldDetail.constantValueIndex != -1 )
            {
                ConstantInteger constantInteger = byteCodeParser.getConstantIntegerMap().get( fieldDetail.constantValueIndex );
                if ( constantInteger != null )
                {
                    if ( fieldType.indexOf( "Z" ) != -1 )
                    {
                        if ( constantInteger.bytes == 0 )
                        {
                            fieldsData.append( " = false" );
                        }
                        else
                        {
                            fieldsData.append( " = true" );
                        }
                    }
                    else
                    {
                        fieldsData.append( " = " + constantInteger.bytes );
                    }
                    break constantValue;
                }
                ConstantFloat constantFloat = byteCodeParser.getConstantFloatMap().get( fieldDetail.constantValueIndex );
                if ( constantFloat != null )
                {
                    //TODO: floating number parsing has to be done
                    break constantValue;
                }
                ConstantDouble constantDouble = byteCodeParser.getConstantDoubleMap().get( fieldDetail.constantValueIndex );
                if ( constantDouble != null )
                {
                    //TODO: double number parsing has to be done
                    break constantValue;
                }
                ConstantString constantString = byteCodeParser.getConstantStringMap().get( fieldDetail.constantValueIndex );
                if ( constantString != null )
                {
                    //TODO: double number parsing has to be done
                    String constant = byteCodeParser.getConstantUtf8Map().get( constantString.stringIndex ).utf8String;
                    fieldsData.append( " = \"" + constant + "\"" );
                    break constantValue;
                }
            }
            fieldsData.append( ";" );

            fieldsData.append( "\n" );
        }
        return fieldsData.toString();
    }
}
