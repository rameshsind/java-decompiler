package javadecompiler.bytecode.parser;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import javadecompiler.bytecode.entities.ByteCodeInstraction;
import javadecompiler.bytecode.entities.CodeLineNumber;
import javadecompiler.bytecode.entities.FieldDetail;
import javadecompiler.bytecode.entities.LocalVariable;
import javadecompiler.bytecode.entities.MethodDetail;
import javadecompiler.bytecode.parser.opcode.JavaOpcodeLoader;
import javadecompiler.bytecode.parser.opcode.Opcode;
import javadecompiler.constantpool.type.ConstantClass;
import javadecompiler.constantpool.type.ConstantDouble;
import javadecompiler.constantpool.type.ConstantFieldref;
import javadecompiler.constantpool.type.ConstantFloat;
import javadecompiler.constantpool.type.ConstantInteger;
import javadecompiler.constantpool.type.ConstantInterfaceMethodref;
import javadecompiler.constantpool.type.ConstantLong;
import javadecompiler.constantpool.type.ConstantMethodref;
import javadecompiler.constantpool.type.ConstantNameAndType;
import javadecompiler.constantpool.type.ConstantString;
import javadecompiler.constantpool.type.ConstantUtf8;
import javadecompiler.exception.DecompilerException;

public class ByteCodeParser
{
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref = 11;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_Utf8 = 1;

    private HashMap< Integer, ConstantClass > ConstantClassMap = null;
    private HashMap< Integer, ConstantFieldref > ConstantFieldrefMap = null;
    private HashMap< Integer, ConstantMethodref > ConstantMethodrefMap = null;
    private HashMap< Integer, ConstantInterfaceMethodref > ConstantInterfaceMethodrefMap = null;
    private HashMap< Integer, ConstantString > ConstantStringMap = null;
    private HashMap< Integer, ConstantInteger > ConstantIntegerMap = null;
    private HashMap< Integer, ConstantFloat > ConstantFloatMap = null;
    private HashMap< Integer, ConstantLong > ConstantLongMap = null;
    private HashMap< Integer, ConstantDouble > ConstantDoubleMap = null;
    private HashMap< Integer, ConstantNameAndType > ConstantNameAndTypeMap = null;
    private HashMap< Integer, ConstantUtf8 > ConstantUtf8Map = null;

    private int[] constPoolinfo;
    public int superClassIndex;
    public int thisClassIndex;
    public String accessFlag;
    public FieldDetail[] fieldDetails = null;
    public int[] interfaceDetails = null;
    public MethodDetail[] methodDetails;
    private Map< String, Opcode > opcodeInfoMap = new HashMap< String, Opcode >();
    private File file;
    private FileInputStream fin;
    private FileWriter fw;
    
    private int exceptionsIndex = -1;
    private int codeIndex = -1;
    private int lineTableIndex = -1;
    private int localVarTableIndex = -1;
    private int constantValueIndex = -1;
    private int signatureIndex = -1;

//    private static final Log log = LogFactory.getLog(ByteCodeParser.class);
    public void extractSuperClass() throws NumberFormatException, IOException
    {
        superClassIndex = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( "Super Class Index  - " + superClassIndex );
    }

    public void extractThisClass() throws NumberFormatException, IOException
    {
        thisClassIndex = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( "This Class Index  - " + thisClassIndex );
    }

    public void extractAccessFlag() throws IOException
    {
        accessFlag = getHexa( 2 );
        logInfo( "Access Flag - " + accessFlag );
    }

    public void extractMethods() throws NumberFormatException, IOException
    {
        int methodCount = Integer.parseInt( getHexa( 2 ), 16 );
        methodDetails = new MethodDetail[methodCount];
        logInfo( "Method Count : " + methodCount );
        for ( int methodIndex = 1; methodIndex <= methodCount; methodIndex++ )
        {
            MethodDetail methodDetail = new MethodDetail();
            methodDetail.index = methodIndex;
            methodDetail.accessFlag = getHexa( 2 );
            methodDetail.nameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            methodDetail.descriptorIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( "Method Index : " + methodIndex );
            logInfo( "        Access Flag - " + methodDetail.accessFlag );
            logInfo( "        Name_Index  - " + methodDetail.nameIndex );
            logInfo( "        Descriptor_Index - " + methodDetail.descriptorIndex );
            extractMethodAttributes( "        ", methodDetail );
            methodDetails[methodIndex - 1] = methodDetail;
        }
    }

    public void extractMethodAttributes( String displaySpace, MethodDetail methodDetail ) throws IOException
    {
        int attributeCount = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "Attribute Count " + attributeCount + " :  " );
        for ( int attributeIndex = 1; attributeIndex <= attributeCount; attributeIndex++ )
        {
            logInfo( displaySpace + "     Attribute Index " + attributeIndex + " :  " );
            int attributeNameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( displaySpace + "              Attribute_name_index - " + attributeNameIndex );
            int attributeLength = Integer.parseInt( getHexa( 4 ), 16 );
            logInfo( displaySpace + "                 Attribute_length  - " + attributeLength );
            if ( attributeNameIndex == codeIndex )
            {
                extractCodeAttribute( displaySpace + "                  ", attributeLength, methodDetail );
            }
            else if ( attributeNameIndex == exceptionsIndex )
            {
                extractExceptionAttribute( displaySpace + "                  ", attributeLength, methodDetail );
            }
            else
            {
                logInfo( displaySpace + "                 Attribute_Value  - " + getHexa( attributeLength ) );
            }
        }
    }

    private void extractExceptionAttribute( String displaySpace, int attributeLength, MethodDetail methodDetail ) throws IOException
    {
        int exceptionCount = Integer.parseInt( getHexa( 2 ), 16 );
        methodDetail.exceptions = new int[exceptionCount];
        for ( int count = 0; count < exceptionCount; count++ )
        {
            methodDetail.exceptions[count] = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( displaySpace + getConstantPoolLookupStr( methodDetail.exceptions[count] ) );
        }

    }

    public void extractAttributes( String displaySpace ) throws IOException
    {
        int attributeCount = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "Attribute Count " + attributeCount + " :  " );
        for ( int attributeIndex = 1; attributeIndex <= attributeCount; attributeIndex++ )
        {
            logInfo( displaySpace + "     Attribute Index " + attributeIndex + " :  " );
            int attributeNameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( displaySpace + "              Attribute_name_index - " + attributeNameIndex );
            int attributeLength = Integer.parseInt( getHexa( 4 ), 16 );
            logInfo( displaySpace + "                 Attribute_length  - " + attributeLength );
            logInfo( displaySpace + "                 Attribute_Value  - " + getHexa( attributeLength ) );
        }
    }

    private void extractSignatureAttribute( String displaySpace, FieldDetail fieldDetail ) throws IOException
    {
        fieldDetail.fieldSignatureIndex = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "SignatureAttribute_Value_index  - " + fieldDetail.fieldSignatureIndex );
    }

    private void extractConstantValueAttribute( String displaySpace, FieldDetail fieldDetail ) throws IOException
    {
        fieldDetail.constantValueIndex = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "Constantvalue_index  - " + fieldDetail.constantValueIndex );
    }

    public void extractLocalVariableTable( String displaySpace, MethodDetail methodDetail ) throws NumberFormatException, IOException
    {
        logInfo( displaySpace + "LocalVariableTable Info:" );
        int lineTableLength = Integer.parseInt( getHexa( 2 ), 16 );
        //methodDetail.localVariables = new LocalVariable[lineTableLength + 10];
        logInfo( displaySpace + "LocalVariableTable Length :" + lineTableLength );
        for ( int tableCount = 1; tableCount <= lineTableLength; tableCount++ )
        {
            LocalVariable localVariable = new LocalVariable();
            localVariable.start_pc = Integer.parseInt( getHexa( 2 ), 16 );
            localVariable.length = Integer.parseInt( getHexa( 2 ), 16 );
            localVariable.name_index = Integer.parseInt( getHexa( 2 ), 16 );
            localVariable.descriptor_index = Integer.parseInt( getHexa( 2 ), 16 );
            localVariable.index = Integer.parseInt( getHexa( 2 ), 16 );

            logInfo( displaySpace + "start_pc :" + localVariable.start_pc );
            logInfo( displaySpace + "length :" + localVariable.length );
            logInfo( displaySpace + "name_index :" + getConstantPoolLookupStr( localVariable.name_index ) );
            logInfo( displaySpace + "descriptor_index :" + getConstantPoolLookupStr( localVariable.descriptor_index ) );
            logInfo( displaySpace + "index :" + localVariable.index );
            List< LocalVariable > localVariableList = methodDetail.indexLocalVariableMap.get( localVariable.index );
            if ( localVariableList == null )
            {
                localVariableList = new ArrayList< LocalVariable >();
                methodDetail.indexLocalVariableMap.put( localVariable.index, localVariableList );
            }
            localVariableList.add( localVariable );
            // methodDetail.localVariables[localVariable.index] = localVariable;
        }
    }

    public void extractLineNumberTable( String displaySpace, MethodDetail methodDetail ) throws NumberFormatException, IOException
    {
        logInfo( displaySpace + "LineNumberTable Info:" );
        int lineTableLength = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "LineNumberTable Length :" + lineTableLength );
        methodDetail.codeLineNumbers = new CodeLineNumber[lineTableLength];
        methodDetail.startPcCodeLineNumberMap = new TreeMap< Integer, CodeLineNumber >();
        for ( int lineTableCount = 1; lineTableCount <= lineTableLength; lineTableCount++ )
        {
            CodeLineNumber codeLineNumber = new CodeLineNumber();
            codeLineNumber.start_pc = Integer.parseInt( getHexa( 2 ), 16 );
            codeLineNumber.line_number = Integer.parseInt( getHexa( 2 ), 16 );
            methodDetail.startPcCodeLineNumberMap.put( codeLineNumber.start_pc, codeLineNumber );
            if ( lineTableCount > 1 )
            {
                if ( methodDetail.codeLineNumbers[lineTableCount - 2].end_pc == -1 )
                {
                    if ( !methodDetail.expHandlePcSet.contains( codeLineNumber.start_pc ) )
                    {
                        methodDetail.codeLineNumbers[lineTableCount - 2].end_pc = codeLineNumber.start_pc - 1;
                    }
                    else
                    {
                        methodDetail.codeLineNumbers[lineTableCount - 2].end_pc = codeLineNumber.start_pc - 4;
                    }
                }
            }
            methodDetail.codeLineNumbers[lineTableCount - 1] = codeLineNumber;
            logInfo( displaySpace + "start_pc :" + codeLineNumber.start_pc + " -- " + "line_number :" + codeLineNumber.line_number );
        }
    }

    public void extractCodeAttribute( String displaySpace, int attributeLength, MethodDetail methodDetail ) throws NumberFormatException, IOException
    {
        logInfo( displaySpace + "Max_Stack :" + Integer.parseInt( getHexa( 2 ), 16 ) );
        logInfo( displaySpace + "Max_Locals :" + Integer.parseInt( getHexa( 2 ), 16 ) );
        int codeLength = Integer.parseInt( getHexa( 4 ), 16 );
        logInfo( displaySpace + "Code Length :" + codeLength );
        methodDetail.byteCodeInstractionList = new ArrayList< ByteCodeInstraction >();
        methodDetail.startPcByteCodeInstractionMap = new TreeMap< Integer, ByteCodeInstraction >();
        ArrayList< String > hexaStringList = new ArrayList< String >();
        int previousInstructionStartPCIndex = -1;
        for ( int count = 1; count <= codeLength; )
        {
            ByteCodeInstraction byteCodeInstraction = new ByteCodeInstraction();
            String hexaStrng = "";
            byteCodeInstraction.opCode = getHexa( 1 );
            hexaStrng += byteCodeInstraction.opCode;
            int instructionStartIndex = count - 1;
            count++;
            Opcode opcode = opcodeInfoMap.get( byteCodeInstraction.opCode );
            int operandCount = 1;
            String constantPoolLookup = "";
            if ( opcode != null )
            {
                byteCodeInstraction.operands = new String[opcode.operandCount];
                byteCodeInstraction.length = opcode.operandCount + 1;
                String operandList = "";
                if ( opcode.description.indexOf( "constant pool" ) > -1 )
                {
                    String hexa = getHexa( 1 );
                    int index = Integer.parseInt( hexa, 16 );
                    byteCodeInstraction.operands[operandCount - 1] = hexa;
                    hexaStrng = hexaStrng + "    " + hexa;
                    operandCount++;
                    count++;
                    if ( opcode.operandCount > 1 )
                    {
                        hexa = getHexa( 1 );
                        byteCodeInstraction.operands[operandCount - 1] = hexa;
                        int secondHalf = Integer.parseInt( hexa, 16 );
                        hexaStrng = hexaStrng + "    " + hexa;
                        operandCount++;
                        count++;
                        index = ( index << 8 ) + secondHalf;
                    }
                    constantPoolLookup = getConstantPoolLookupStr( index );
                }
                boolean isBranchingStatement = false;
                if ( opcode.opcode.indexOf( "if" ) > -1 || opcode.opcode.indexOf( "goto" ) > -1 )
                {
                    isBranchingStatement = true;
                }
                for ( ; operandCount <= opcode.operandCount; operandCount++ )
                {
                    byteCodeInstraction.operands[operandCount - 1] = getHexa( 1 );
                    operandList = operandList + "    " + byteCodeInstraction.operands[operandCount - 1];
                    count++;
                }
                short branch = 0;
                if ( isBranchingStatement && opcode.operandCount == 2 )
                {
                    short byte1 = Short.parseShort( byteCodeInstraction.operands[0], 16 );
                    short byte2 = Short.parseShort( byteCodeInstraction.operands[1], 16 );
                    branch = (short)( ( byte1 << (short)8 ) + byte2 );
                    byteCodeInstraction.branchOffset = branch;
                }
                hexaStrng = hexaStrng + operandList;
                byteCodeInstraction.startPCIndex = instructionStartIndex;
                String codeInfo = getInstructionIndex( instructionStartIndex ) + opcode.opcode + "    " + constantPoolLookup + "	" + operandList + "   " + ( ( branch != 0 ) ? branch : "" );
                codeInfo = getFormatedLine( codeInfo, opcode.description );
                logInfo( displaySpace + codeInfo + opcode.description );
            }
            else
            {
                logInfo( "Error in parsing..." );
            }
            hexaStringList.add( hexaStrng );
            methodDetail.byteCodeInstractionList.add( byteCodeInstraction );
            byteCodeInstraction.previousInstructionStartPCIndex = previousInstructionStartPCIndex;
            methodDetail.startPcByteCodeInstractionMap.put( byteCodeInstraction.startPCIndex, byteCodeInstraction );
            previousInstructionStartPCIndex = byteCodeInstraction.startPCIndex;
        }
        logInfo( displaySpace + "Method in hexa representation .." );
        for ( String hexastr : hexaStringList )
        {
            logInfo( displaySpace + hexastr );
        }
        // logInfo(displaySpace + "  Remaining Attribute Info :"+
        // getHexa(attributeLength - codeLength - 8));
        extractExceptionTableInfo( displaySpace, methodDetail );
        extractCodeSubAttributes( displaySpace, methodDetail );
    }

    private String getInstructionIndex( int instructionStartIndex )
    {
        String str = "" + instructionStartIndex;
        for ( int i = str.length(); i < 5; i++ )
        {
            str += " ";
        }
        return str;
    }

    private void extractCodeSubAttributes( String displaySpace, MethodDetail methodDetail ) throws IOException
    {
        int attributeCount = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "Attribute Count " + attributeCount + " :  " );
        for ( int attributeIndex = 1; attributeIndex <= attributeCount; attributeIndex++ )
        {
            logInfo( displaySpace + "     Attribute Index " + attributeIndex + " :  " );
            int attributeNameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( displaySpace + "              Attribute_name_index - " + attributeNameIndex );
            int attributeLength = Integer.parseInt( getHexa( 4 ), 16 );
            logInfo( displaySpace + "                 Attribute_length  - " + attributeLength );
            if ( attributeNameIndex == lineTableIndex )
            {
                extractLineNumberTable( displaySpace + "                  ", methodDetail );
            }
            else if ( attributeNameIndex == localVarTableIndex )
            {
                extractLocalVariableTable( displaySpace + "                  ", methodDetail );
            }
            else
            {
                logInfo( displaySpace + "                 Attribute_Value  - " + getHexa( attributeLength ) );
            }
        }
    }

    public void extractExceptionTableInfo( String displaySpace, MethodDetail methodDetail ) throws NumberFormatException, IOException
    {
        int exceptionTableLen = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + " Exception Table Length : " + exceptionTableLen );
        for ( int lineexceptionTableCount = 1; lineexceptionTableCount <= exceptionTableLen; lineexceptionTableCount++ )
        {
            int start_pc = Integer.parseInt( getHexa( 2 ), 16 );
            int end_pc = Integer.parseInt( getHexa( 2 ), 16 );
            int handler_pc = Integer.parseInt( getHexa( 2 ), 16 );
            int catch_type = Integer.parseInt( getHexa( 2 ), 16 );
            methodDetail.tryStartPcSet.add( start_pc );
            methodDetail.tryEndStartPcSet.add( end_pc );
            methodDetail.expHandlePcSet.add( handler_pc );
            logInfo( displaySpace + "start_pc :" + start_pc + " -- " + "end_pc :" + end_pc );
            logInfo( displaySpace + "handler_pc :" + handler_pc );
            logInfo( displaySpace + "catch_type :" + getConstantPoolLookupStr( catch_type ) );
        }
    }

    public String getFormatedLine( String codeInfo, String description )
    {
        int paddingLen = 80 - codeInfo.length();
        StringBuilder result = new StringBuilder( codeInfo );
        for ( int i = 0; i < paddingLen; i++ )
            result.append( " " );
        return result.toString();
    }

    public String getConstantPoolLookupStr( int index )
    {
        switch ( constPoolinfo[index] )
        {
            case CONSTANT_Class:

                ConstantClass cc = ConstantClassMap.get( index );
                return getConstantPoolLookupStr( cc.nameIndex );

            case CONSTANT_Fieldref:

                ConstantFieldref cfr = ConstantFieldrefMap.get( index );
                String className = getConstantPoolLookupStr( cfr.classIndex );
                String fieldName = getConstantPoolLookupStr( cfr.name_and_type_index );
                return " " + className + " " + fieldName;

            case CONSTANT_Methodref:

                ConstantMethodref cmr = ConstantMethodrefMap.get( index );
                String className1 = getConstantPoolLookupStr( cmr.classIndex );
                String methodName1 = getConstantPoolLookupStr( cmr.name_and_type_index );
                return " " + className1 + " " + methodName1;

            case CONSTANT_InterfaceMethodref:
                ConstantInterfaceMethodref cimr = ConstantInterfaceMethodrefMap.get( index );
                String className2 = getConstantPoolLookupStr( cimr.classIndex );
                String methodName2 = getConstantPoolLookupStr( cimr.name_and_type_index );
                return " " + className2 + " " + methodName2;

            case CONSTANT_String:
                ConstantString cs = ConstantStringMap.get( index );
                return getConstantPoolLookupStr( cs.stringIndex );

            case CONSTANT_Integer:
                ConstantInteger ci = ConstantIntegerMap.get( index );
                return " " + ci.bytes;

            case CONSTANT_Float:

                ConstantFloat cf = ConstantFloatMap.get( index );
                return " " + cf.bytes;

            case CONSTANT_Long:

                ConstantLong cl = ConstantLongMap.get( index );
                return " " + cl.bytes;

            case CONSTANT_Double:

                ConstantDouble cd = ConstantDoubleMap.get( index );
                return " " + cd.bytes;

            case CONSTANT_NameAndType:

                ConstantNameAndType cnat = ConstantNameAndTypeMap.get( index );
                String name = getConstantPoolLookupStr( cnat.nameIndex );
                String descritor = getConstantPoolLookupStr( cnat.descriptor_index );
                return name + " " + descritor;

            case CONSTANT_Utf8:
                ConstantUtf8 data = ConstantUtf8Map.get( index );
                return data.utf8String;

            default:
                System.out.println( "No Tag Matched" );
                break;

        }
        return null;
    }

    public void extractFields() throws NumberFormatException, IOException
    {
        int fieldCount = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( "Field Count : " + fieldCount );
        fieldDetails = new FieldDetail[fieldCount];
        for ( int fieldIndex = 1; fieldIndex <= fieldCount; fieldIndex++ )
        {
            FieldDetail fieldDetail = new FieldDetail();
            fieldDetail.index = fieldIndex;
            fieldDetail.accessFlag = getHexa( 2 );
            fieldDetail.nameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            fieldDetail.descriptorIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( "Field Index " + fieldIndex + " :  " );
            logInfo( "        Access Flag - " + fieldDetail.accessFlag );
            logInfo( "        Name_Index  - " + fieldDetail.nameIndex );
            logInfo( "        Descriptor_Index - " + fieldDetail.descriptorIndex );
            extractFieldAttributes( "        ", fieldDetail );
            fieldDetails[fieldIndex - 1] = fieldDetail;
        }
    }

    private void extractFieldAttributes( String displaySpace, FieldDetail fieldDetail ) throws IOException
    {
        int attributeCount = Integer.parseInt( getHexa( 2 ), 16 );
        logInfo( displaySpace + "Attribute Count " + attributeCount + " :  " );
        for ( int attributeIndex = 1; attributeIndex <= attributeCount; attributeIndex++ )
        {
            logInfo( displaySpace + "     Attribute Index " + attributeIndex + " :  " );
            int attributeNameIndex = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( displaySpace + "              Attribute_name_index - " + attributeNameIndex );
            int attributeLength = Integer.parseInt( getHexa( 4 ), 16 );
            logInfo( displaySpace + "                 Attribute_length  - " + attributeLength );
            if ( attributeNameIndex == constantValueIndex )
            {
                extractConstantValueAttribute( displaySpace + "                  ", fieldDetail );
            }
            else if ( attributeNameIndex == signatureIndex )
            {
                extractSignatureAttribute( displaySpace + "                  ", fieldDetail );
            }
            else
            {
                logInfo( displaySpace + "                 Attribute_Value  - " + getHexa( attributeLength ) );
            }
        }
    }

    public void extractInterfaces() throws NumberFormatException, IOException
    {
        int interfaceCount = Integer.parseInt( getHexa( 2 ), 16 );
        if ( interfaceCount < 1 )
            return;
        interfaceDetails = new int[interfaceCount];
        logInfo( "Interface Count : " + interfaceCount );

        for ( int interfaceIndex = 1; interfaceIndex <= interfaceCount; interfaceIndex++ )
        {
            interfaceDetails[interfaceIndex - 1] = Integer.parseInt( getHexa( 2 ), 16 );
            logInfo( "Interface Index " + interfaceIndex + " :  " + " Name Index - " + interfaceDetails[interfaceIndex - 1] );
        }
    }

    public void extractConstantPool() throws NumberFormatException, IOException
    {
        int constPoolLength = Integer.parseInt( getHexa( 2 ), 16 );
        constPoolinfo = new int[constPoolLength];
        intializeConstPoolDS();
        logInfo( "Constant Pool Length : " + constPoolLength );
        for ( int constPoolIndex = 1; constPoolIndex < constPoolLength; constPoolIndex++ )
        {
            int constantPoolTag = Integer.parseInt( getHexa( 1 ), 16 );
            constPoolinfo[constPoolIndex] = constantPoolTag;
            switch ( constantPoolTag )
            {
                case CONSTANT_Class:

                    int nameIndex = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Class : " + " Name Index - " + nameIndex );
                    ConstantClassMap.put( constPoolIndex, new ConstantClass( nameIndex ) );
                    break;

                case CONSTANT_Fieldref:

                    int classIndex = Integer.parseInt( getHexa( 2 ), 16 );
                    int name_and_type_index = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Fieldref : " + " class_index - " + classIndex + ": " + " name_and_type_index - " + name_and_type_index );
                    ConstantFieldrefMap.put( constPoolIndex, new ConstantFieldref( classIndex, name_and_type_index ) );
                    break;

                case CONSTANT_Methodref:

                    int classIndex1 = Integer.parseInt( getHexa( 2 ), 16 );
                    int name_and_type_index1 = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Methodref : " + " class_index - " + classIndex1 + ": " + " name_and_type_index - " + name_and_type_index1 );
                    ConstantMethodrefMap.put( constPoolIndex, new ConstantMethodref( classIndex1, name_and_type_index1 ) );
                    break;

                case CONSTANT_InterfaceMethodref:

                    int classIndex2 = Integer.parseInt( getHexa( 2 ), 16 );
                    int name_and_type_index2 = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_InterfaceMethodref : " + " class_index - " + classIndex2 + ": " + " name_and_type_index - " + name_and_type_index2 );
                    ConstantInterfaceMethodrefMap.put( constPoolIndex, new ConstantInterfaceMethodref( classIndex2, name_and_type_index2 ) );
                    break;

                case CONSTANT_String:

                    int stringIndex = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_String : " + " string_index - " + stringIndex );
                    ConstantStringMap.put( constPoolIndex, new ConstantString( stringIndex ) );
                    break;

                case CONSTANT_Integer:

                    int bytes = Integer.parseInt( getHexa( 4 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Integer : " + " bytes - " + bytes );
                    ConstantIntegerMap.put( constPoolIndex, new ConstantInteger( bytes ) );
                    break;

                case CONSTANT_Float:

                    bytes = Integer.parseInt( getHexa( 4 ), 16 );
                    int s = ( ( bytes >> 31 ) == 0 ) ? 1 : -1;
                    int e = ( ( bytes >> 23 ) & 0xff );
                    int m = ( e == 0 ) ? ( bytes & 0x7fffff ) << 1 : ( bytes & 0x7fffff ) | 0x800000;
                    //s·m·2e-150
                    float bytesFloat = (float)( s * m * Math.pow( 2, e - 150 ) );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Float : " + " bytes - " + bytesFloat );
                    ConstantFloatMap.put( constPoolIndex, new ConstantFloat( bytesFloat ) );
                    break;

                case CONSTANT_Long:

                    String highBytes = getHexa( 4 );
                    String lowBytes = getHexa( 4 );
                    long bytesLong = Long.parseLong( highBytes + lowBytes, 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Long : " + " high_bytes - " + highBytes + " low_bytes - " + lowBytes );
                    ConstantLongMap.put( constPoolIndex, new ConstantLong( bytesLong ) );
                    constPoolIndex++;//longest number continued
                    break;

                case CONSTANT_Double:

                    highBytes = getHexa( 4 );
                    lowBytes = getHexa( 4 );
                    bytesLong = Long.parseLong( highBytes + lowBytes, 16 );
                    s = ( ( bytesLong >> 63 ) == 0 ) ? 1 : -1;
                    e = (int)( ( bytesLong >> 52 ) & 0x7ffL );
                    long mL = ( e == 0 ) ? ( bytesLong & 0xfffffffffffffL ) << 1 : ( bytesLong & 0xfffffffffffffL ) | 0x10000000000000L;
                    //s·m·2e-1075
                    double bytesDouble = ( s * mL * Math.pow( 2, e - 1075 ) );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Double :  high_bytes - " + highBytes + " low_bytes - " + lowBytes + " value - " + bytesDouble );
                    ConstantDoubleMap.put( constPoolIndex, new ConstantDouble( bytesDouble ) );
                    constPoolIndex++;//longest number continued
                    break;

                case CONSTANT_NameAndType:

                    int nameIndex1 = Integer.parseInt( getHexa( 2 ), 16 );
                    int descriptor_index = Integer.parseInt( getHexa( 2 ), 16 );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_NameAndType : " + " name_index - "  + nameIndex1 + " descriptor_index - " + descriptor_index );
                    ConstantNameAndTypeMap.put( constPoolIndex, new ConstantNameAndType( nameIndex1, descriptor_index ) );
                    break;

                case CONSTANT_Utf8:

                    int utfLen = Integer.parseInt( getHexa( 2 ), 16 );
                    String utf8String = getString( utfLen );
                    logInfo( "Index " + constPoolIndex + " : CONSTANT_Utf8 : " + " length - " + utfLen + " bytes - " + utf8String );
                    if ( utf8String.equals( "Code" ) )
                    {
                        codeIndex = constPoolIndex;
                    }
                    else if ( utf8String.equals( "LineNumberTable" ) )
                    {
                        lineTableIndex = constPoolIndex;
                    }
                    else if ( utf8String.equals( "LocalVariableTable" ) )
                    {
                        localVarTableIndex = constPoolIndex;
                    }
                    else if ( utf8String.equals( "ConstantValue" ) )
                    {
                        constantValueIndex = constPoolIndex;
                    }
                    else if ( utf8String.equals( "Signature" ) )
                    {
                        signatureIndex = constPoolIndex;
                    }
                    else if ( utf8String.equals( "Exceptions" ) )
                    {
                        exceptionsIndex = constPoolIndex;
                    }
                    ConstantUtf8Map.put( constPoolIndex, new ConstantUtf8( utfLen, utf8String ) );
                    break;

                default:

                    System.out.println( "No Tag Matched" );
                    break;
            }
        }
    }

    public void intializeConstPoolDS()
    {
        ConstantClassMap = new HashMap< Integer, ConstantClass >();
        ConstantDoubleMap = new HashMap< Integer, ConstantDouble >();
        ConstantFieldrefMap = new HashMap< Integer, ConstantFieldref >();
        ConstantFloatMap = new HashMap< Integer, ConstantFloat >();
        ConstantIntegerMap = new HashMap< Integer, ConstantInteger >();
        ConstantInterfaceMethodrefMap = new HashMap< Integer, ConstantInterfaceMethodref >();
        ConstantLongMap = new HashMap< Integer, ConstantLong >();
        ConstantMethodrefMap = new HashMap< Integer, ConstantMethodref >();
        ConstantNameAndTypeMap = new HashMap< Integer, ConstantNameAndType >();
        ConstantStringMap = new HashMap< Integer, ConstantString >();
        ConstantUtf8Map = new HashMap< Integer, ConstantUtf8 >();
    }

    public void extractMajMinNo() throws IOException
    {
        String minNo = getHexa( 2 );
        logInfo( "Minnor No : " + Integer.parseInt( minNo, 16 ) );
        String majNo = getHexa( 2 );
        logInfo( "Major No : " + Integer.parseInt( majNo, 16 ) );
    }

    public void finalise() throws IOException
    {
        if ( fin != null )
            fin.close();
        
        if (fw != null )
        {
            fw.flush();
            fw.close();
        }
    }

    public void extractMagicNo() throws IOException
    {
        String magicNo = getHexa( 4 );
        logInfo( "Magic No : " + magicNo );
    }

    public void logInfo( String string )
    {
       
        System.out.println( string );
        if (fw != null )
        {
            try
            {
                fw.write( string );
                fw.write( "\n" );
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public String getString( int length ) throws IOException
    {
        String result = "";
        for ( int count = 0; count < length; count++ )
        {
            int ch = fin.read();
            result += (char)ch;
        }
        return result;
    }

    public String getHexa( int length ) throws IOException
    {
        String result = "";
        for ( int count = 0; count < length; count++ )
        {
            int ch = fin.read();
            result += ParseHelper.padWithZero( java.lang.Integer.toHexString( ch ) );
        }
        return result;
    }

    public void intialize( String inputClassFileName, boolean createByteCodeFile, String byteCodeInstructionFile ) throws  DecompilerException
    {
        try
        {
            file = new File( inputClassFileName );
            fin = new FileInputStream( file );
            JavaOpcodeLoader.loadJVMInsruction( opcodeInfoMap, byteCodeInstructionFile );
            if( createByteCodeFile )
            {
              fw = new FileWriter( inputClassFileName + "bytecode.txt" );
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new DecompilerException(e);
        }
    }

    public Map< String, Opcode > getOpcodeInfoMap()
    {
        return opcodeInfoMap;
    }

    public HashMap< Integer, ConstantClass > getConstantClassMap()
    {
        return ConstantClassMap;
    }

    public HashMap< Integer, ConstantFieldref > getConstantFieldrefMap()
    {
        return ConstantFieldrefMap;
    }

    public HashMap< Integer, ConstantMethodref > getConstantMethodrefMap()
    {
        return ConstantMethodrefMap;
    }

    public HashMap< Integer, ConstantInterfaceMethodref > getConstantInterfaceMethodrefMap()
    {
        return ConstantInterfaceMethodrefMap;
    }

    public HashMap< Integer, ConstantString > getConstantStringMap()
    {
        return ConstantStringMap;
    }

    public HashMap< Integer, ConstantInteger > getConstantIntegerMap()
    {
        return ConstantIntegerMap;
    }

    public HashMap< Integer, ConstantFloat > getConstantFloatMap()
    {
        return ConstantFloatMap;
    }

    public HashMap< Integer, ConstantLong > getConstantLongMap()
    {
        return ConstantLongMap;
    }

    public HashMap< Integer, ConstantDouble > getConstantDoubleMap()
    {
        return ConstantDoubleMap;
    }

    public HashMap< Integer, ConstantNameAndType > getConstantNameAndTypeMap()
    {
        return ConstantNameAndTypeMap;
    }

    public HashMap< Integer, ConstantUtf8 > getConstantUtf8Map()
    {
        return ConstantUtf8Map;
    }

    public int[] getConstPoolinfo()
    {
        return constPoolinfo;
    }

    public int getSuperClassIndex()
    {
        return superClassIndex;
    }

    public int getThisClassIndex()
    {
        return thisClassIndex;
    }

    public String getAccessFlag()
    {
        return accessFlag;
    }

    public FieldDetail[] getFieldDetails()
    {
        return fieldDetails;
    }

    public MethodDetail[] getMethodDetails()
    {
        return methodDetails;
    }

    public int[] getInterfaceDetails()
    {
        return interfaceDetails;
    }
}