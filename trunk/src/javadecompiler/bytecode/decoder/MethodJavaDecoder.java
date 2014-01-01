package javadecompiler.bytecode.decoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javadecompiler.bytecode.entities.ByteCodeInstraction;
import javadecompiler.bytecode.entities.CodeLineNumber;
import javadecompiler.bytecode.entities.LocalVariable;
import javadecompiler.bytecode.entities.MethodCodeLine;
import javadecompiler.bytecode.entities.MethodDetail;
import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.constantpool.type.ConstantClass;
import javadecompiler.constantpool.type.ConstantFieldref;
import javadecompiler.constantpool.type.ConstantMethodref;
import javadecompiler.constantpool.type.ConstantNameAndType;
import javadecompiler.constantpool.type.ConstantUtf8;


public class MethodJavaDecoder
{
    private static final String DOUBLE_ONE = "1.0d";
    private static final String FLOAT_ONE = "1.0f";
    boolean isConditionalStarted = false;
    boolean isCompare = false;
    String conditionalType = null;
    private ByteCodeInstraction conditionalJumpedByteCodeInst = null;
    private ByteCodeInstraction conditionalPrevByteCodeInst = null;
    Stack< String > methodStack;

    private ByteCodeParser byteCodeParser;
    BytecodeToJavaDecoder bytecodeToJavaDecoder;
    public MethodJavaDecoder( ByteCodeParser byteCodeParser, BytecodeToJavaDecoder bytecodeToJavaDecoder )
    {
        this.byteCodeParser = byteCodeParser;
        this.bytecodeToJavaDecoder = bytecodeToJavaDecoder;
    }
    public String decode()
    {
        MethodDetail[] methodDetails = byteCodeParser.getMethodDetails();
        if ( methodDetails == null )
            return "";
        StringBuilder methodData = new StringBuilder();
       
        for ( MethodDetail methodDetail : methodDetails )
        {
            
            methodStack = new Stack< String >();
            Map< Integer, List< MethodCodeLine > > startPCIndexMethodCodeMap = new TreeMap< Integer, List< MethodCodeLine > >();
            String accessSpecifier = DecodeHelper.getAccessSpecifier( methodDetail.accessFlag, true );

            boolean isStatic = accessSpecifier.indexOf( " static " ) != -1;
            methodData.append( DecodeHelper.FORMATTED_SPACE ).append( accessSpecifier );
            String methodName = byteCodeParser.getConstantUtf8Map().get( methodDetail.nameIndex ).utf8String;
            boolean isConstructor = methodName.equals( "<init>" );
            String[] methodSignature = getMethodSignature( byteCodeParser.getConstantUtf8Map().get( methodDetail.descriptorIndex ).utf8String, methodDetail, isStatic, isConstructor );
            methodData.append( methodSignature[0] ).append( " " );
            if ( isConstructor )
            {
                methodData.append( bytecodeToJavaDecoder.packageAndClassName[1] );
            }
            else
            {
                methodData.append( methodName );
            }
            methodData.append( methodSignature[1] );
            addExceptionDetails( methodData, methodDetail );
            methodData.append( "\n" + DecodeHelper.FORMATTED_SPACE + "{\n" );
            int lineNumberIndex = 0;
            int startPCIndex = 0;
            int pc = -1;
            int byteCodeInstractionIndex = 1;
            boolean createLine = false;
            addTryIfRequired( methodDetail, startPCIndexMethodCodeMap, startPCIndex );
            for ( ByteCodeInstraction byteCodeInstraction : methodDetail.byteCodeInstractionList )
            {
                try
                {
                if ( createLine )
                {
                    startPCIndex = byteCodeInstraction.startPCIndex;
                    addTryIfRequired( methodDetail, startPCIndexMethodCodeMap, startPCIndex );
                    createLine = false;
                }

                pc += byteCodeInstraction.length;
                if ( byteCodeInstractionIndex == methodDetail.byteCodeInstractionList.size() || methodDetail.codeLineNumbers[lineNumberIndex].end_pc == pc )
                {
                    lineNumberIndex++;
                    createLine = true;
                }
                processByteCodeInstraction( methodDetail, byteCodeInstraction, createLine, startPCIndexMethodCodeMap, isConstructor, startPCIndex );
                }catch(Throwable t)
                {
                    t.printStackTrace();
                }
                byteCodeInstractionIndex++;
                
            }
            
            int bracketCount = 0;
            for ( List< MethodCodeLine > methodCodeLineList : startPCIndexMethodCodeMap.values() )
            {

                for ( MethodCodeLine methodCodeLine : methodCodeLineList )
                {
                    if ( methodCodeLine.isCurlyBracketEnded )
                    {
                        bracketCount--;
                    }
                    String formatedSpace = DecodeHelper.getFormatedSpace( bracketCount );

                    methodData.append( DecodeHelper.FORMATTED_SPACE ).append( DecodeHelper.FORMATTED_SPACE ).append( formatedSpace ).append( methodCodeLine.codeLine.toString() );
                    if ( methodCodeLine.isCurlyBracketStarted )
                    {
                        bracketCount++;
                    }
                }

            }
            methodData.append( DecodeHelper.FORMATTED_SPACE ).append( "}\n" );
        }
        
        return methodData.toString();
    }
    private void addTryIfRequired( MethodDetail methodDetail, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex )
    {
        if ( methodDetail.tryStartPcSet.contains( startPCIndex ) )
        {
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "try\n" ) ) );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
        }
    }

    private void addExceptionDetails( StringBuilder methodData, MethodDetail methodDetail )
    {
        if ( methodDetail.exceptions == null )
        {
            return;
        }
        for ( int count = 0; count < methodDetail.exceptions.length; count++ )
        {
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( methodDetail.exceptions[count] );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            if ( count == 0 )
            {
                methodData.append( " throws " );
            }
            else
            {
                methodData.append( ", " );
            }
            methodData.append( className );
        }

    }

    private String[] getMethodSignature( String methodDescriptor, MethodDetail methodDetail, boolean isStatic, boolean isConstructor )
    {

        String[] methodSignature = { "", "" };
        if ( !isConstructor )
        {
            String returnType = methodDescriptor.substring( methodDescriptor.lastIndexOf( ')' ) + 1 );
            methodSignature[0] = bytecodeToJavaDecoder.getFormatedFieldType( returnType );
        }
        String argumentsStr = methodDescriptor.substring( methodDescriptor.indexOf( '(' ) + 1, methodDescriptor.lastIndexOf( ')' ) );
        String argVal = "(";
        if ( !argumentsStr.equals( "" ) )
        {
            String[] args = argumentsStr.split( ";" );
            int index = 0;
            int len = args.length;
            if ( !isStatic )
            {
                index++;
                len++;
            }

            for ( ; index < args.length; index++ )
            {
                if ( !argVal.equals( "(" ) )
                    argVal += ", ";
                LocalVariable localVariable = getLocalVariable( methodDetail, index, 0 );
                String varType = byteCodeParser.getConstantUtf8Map().get( localVariable.descriptor_index ).utf8String;
                argVal += bytecodeToJavaDecoder.getFormatedFieldType( varType );
                argVal += " ";
                argVal += getNameFromNameIndex( localVariable.name_index );
                localVariable.isDeclared = true;
            }
        }
        argVal += ")";
        methodSignature[1] = argVal;
        return methodSignature;
    }

    private LocalVariable getLocalVariable( MethodDetail methodDetail, int index, int startPCIndex )
    {
        List< LocalVariable > localVariableList = methodDetail.indexLocalVariableMap.get( index );
        for ( LocalVariable localVariable : localVariableList )
        {
            //TODO: need to find why are we adding +2 and +1
            if ( startPCIndex + 2 >= localVariable.start_pc && startPCIndex <= ( localVariable.start_pc + 1 + localVariable.length ) )
            {
                return localVariable;
            }
        }
        System.out.println( "Unable to find the local variable for the index " + index + " which starts with " + startPCIndex );
        return null;
    }

    private void processByteCodeInstraction( MethodDetail methodDetail, ByteCodeInstraction byteCodeInstraction, boolean createLine, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, boolean isConstructor, int startPCIndex )
    {
        if ( byteCodeInstraction.opCode.equals( "2A" ) || byteCodeInstraction.opCode.equals( "2B" ) || byteCodeInstraction.opCode.equals( "2C" ) || byteCodeInstraction.opCode.equals( "2D" ) || byteCodeInstraction.opCode.equals( "1A" ) || byteCodeInstraction.opCode.equals( "1B" ) || byteCodeInstraction.opCode.equals( "1C" ) || byteCodeInstraction.opCode.equals( "1D" ) )
        {
            char c = byteCodeInstraction.opCode.charAt( 1 );
            LocalVariable localVar = getLocalVariable( methodDetail, c - 65, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            if ( isLocalVarPreIncreement( localVariable ))
                return;
            methodStack.push( localVariable );
        }
        else if ( byteCodeInstraction.opCode.equals( "1E" ) || byteCodeInstraction.opCode.equals( "1F" ) || byteCodeInstraction.opCode.equals( "20" ) || byteCodeInstraction.opCode.equals( "21" ) )
        {
            char c = byteCodeInstraction.opCode.charAt( 1 );
            int index = 0;
            if ( c == 'F' )
            {
                index = 1;
            }
            else if ( c == '0' )
            {
                index = 2;
            }
            else if ( c == '1' )
            {
                index = 3;
            }
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            if ( isLocalVarPreIncreement( localVariable ))
                return;
            methodStack.push( localVariable );
        }
        else if ( byteCodeInstraction.opCode.equals( "22" ) || byteCodeInstraction.opCode.equals( "23" ) || byteCodeInstraction.opCode.equals( "24" ) || byteCodeInstraction.opCode.equals( "25" ) )
        {
            char c = byteCodeInstraction.opCode.charAt( 1 );
            int index = Integer.parseInt( "" + c ) - 2;
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            if ( isLocalVarPreIncreement( localVariable ))
                return;
            methodStack.push( localVariable );
        }
        else if ( byteCodeInstraction.opCode.equals( "26" ) || byteCodeInstraction.opCode.equals( "27" ) || byteCodeInstraction.opCode.equals( "28" ) || byteCodeInstraction.opCode.equals( "29" ) )
        {
            char c = byteCodeInstraction.opCode.charAt( 1 );
            int index = Integer.parseInt( "" + c ) - 6;
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            if ( isLocalVarPreIncreement( localVariable ))
                return;
            methodStack.push( localVariable );
        }
        else if ( byteCodeInstraction.opCode.equals( "15" ) || byteCodeInstraction.opCode.equals( "16" ) || byteCodeInstraction.opCode.equals( "17" ) || byteCodeInstraction.opCode.equals( "18" ) || byteCodeInstraction.opCode.equals( "19" ) )
        {
            int index = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            if ( isLocalVarPreIncreement( localVariable ))
                return;
            methodStack.push( localVariable );
        }
        else if ( byteCodeInstraction.opCode.equals( "57" ) || byteCodeInstraction.opCode.equals( "58" ) )
        {
            if ( byteCodeInstraction.opCode.equals( "58" ) )
            {
                methodStack.pop();
            }
            String data = methodStack.pop();
            if ( createLine )
            {
                List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( data ).append( ";\n" ) ) );

            }
        }
        else if ( byteCodeInstraction.opCode.equals( "4B" ) || byteCodeInstraction.opCode.equals( "4C" ) || byteCodeInstraction.opCode.equals( "4D" ) || byteCodeInstraction.opCode.equals( "4E" ) || byteCodeInstraction.opCode.equals( "3B" ) || byteCodeInstraction.opCode.equals( "3C" ) || byteCodeInstraction.opCode.equals( "3D" ) || byteCodeInstraction.opCode.equals( "3E" ) )
        {
            if ( byteCodeInstraction.opCode.equals( "4D" ) && methodDetail.expHandlePcSet.contains( byteCodeInstraction.startPCIndex ) )
            {
                char c = byteCodeInstraction.opCode.charAt( 1 );
                LocalVariable localVar = getLocalVariable( methodDetail, c - 66, byteCodeInstraction.startPCIndex );
                if ( localVar == null )
                    return;
                String localVariable = getNameFromNameIndex( localVar.name_index );
                String varType = "";
                if ( !localVar.isDeclared )
                {
                    localVar.isDeclared = true;
                    varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                    varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
                }
                List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "} catch( " ).append( varType ).append( " " ).append( localVariable ).append( " ) " ).append( "\n" ), false, true ) );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );

            }
            else
            {
                char c = byteCodeInstraction.opCode.charAt( 1 );
                LocalVariable localVar = getLocalVariable( methodDetail, c - 66, byteCodeInstraction.startPCIndex );
                if ( localVar == null )
                    return;
                String localVariable = getNameFromNameIndex( localVar.name_index );
                String value = methodStack.pop();
                String varType = "";
                if ( !localVar.isDeclared )
                {
                    localVar.isDeclared = true;
                    varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                    boolean isBooleanType = varType.equals( "Z" );
                    varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
                    if ( isBooleanType )
                    {
                        if ( value.equals( "0" ) )
                        {
                            value = "false";
                        }
                        else if ( value.equals( "1" ) )
                        {
                            value = "true";
                        }
                    }
                }
                StringBuilder data = new StringBuilder();
                handleIncreement( localVariable, varType, value, data, "1" );
                finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, data );

            }
        }
        else if ( byteCodeInstraction.opCode.equals( "3F" ) || byteCodeInstraction.opCode.equals( "40" ) || byteCodeInstraction.opCode.equals( "41" ) || byteCodeInstraction.opCode.equals( "42" ) )
        {
            int digit = Integer.parseInt( "" + byteCodeInstraction.opCode.charAt( 1 ) );
            int index = 0;
            if ( digit >= 0 && digit <= 2 )
            {
                index = digit + 1;
            }
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );

            String varType = "";
            if ( !localVar.isDeclared )
            {
                localVar.isDeclared = true;
                varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
            }
            String expression = methodStack.pop();
            StringBuilder data = new StringBuilder();
            handleIncreement( localVariable, varType, expression, data, "1" );
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, data );            
        }
        else if ( byteCodeInstraction.opCode.equals( "43" ) || byteCodeInstraction.opCode.equals( "44" ) || byteCodeInstraction.opCode.equals( "45" ) || byteCodeInstraction.opCode.equals( "46" ) || byteCodeInstraction.opCode.equals( "47" ) || byteCodeInstraction.opCode.equals( "48" ) || byteCodeInstraction.opCode.equals( "49" ) || byteCodeInstraction.opCode.equals( "4A" ) )
        {
            int digit = Integer.parseInt( "" + byteCodeInstraction.opCode.charAt( 1 ), 16 );
            int index = 0;
            if ( digit >= 3 && digit <= 6 )
            {
                index = digit - 3;
            }
            else
            {
                index = digit - 7;
            }
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );

            String varType = "";
            if ( !localVar.isDeclared )
            {
                localVar.isDeclared = true;
                varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
            }
            String expression = methodStack.pop();
            StringBuilder data = new StringBuilder();
            String constantValueOne = "";
            if(byteCodeInstraction.opCode.equals( "43" ) || byteCodeInstraction.opCode.equals( "44" ) || byteCodeInstraction.opCode.equals( "45" ) || byteCodeInstraction.opCode.equals( "46" ))
            {
                constantValueOne = FLOAT_ONE;
            }
            else
            {
                constantValueOne = DOUBLE_ONE;
            }
            handleIncreement( localVariable, varType, expression, data, constantValueOne );
            
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, data );
        }
        else if ( byteCodeInstraction.opCode.equals( "3A" ) || byteCodeInstraction.opCode.equals( "39" ) || byteCodeInstraction.opCode.equals( "38" ) || byteCodeInstraction.opCode.equals( "37" ) || byteCodeInstraction.opCode.equals( "36" ) )
        {
            int index = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            LocalVariable localVar = getLocalVariable( methodDetail, index, byteCodeInstraction.startPCIndex );
            String localVariable = getNameFromNameIndex( localVar.name_index );
            String varType = "";
            String value = methodStack.pop();
            if ( !localVar.isDeclared )
            {
                localVar.isDeclared = true;
                varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                boolean isBooleanType = varType.equals( "Z" );
                varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
                if ( isBooleanType )
                {
                    if ( value.equals( "0" ) )
                    {
                        value = "false";
                    }
                    else if ( value.equals( "1" ) )
                    {
                        value = "true";
                    }
                }
            }
            
            StringBuilder data = new StringBuilder();
            String constantValueOne = "1";
            if(byteCodeInstraction.opCode.equals( "38" ))
            {
                constantValueOne = FLOAT_ONE;
            } else if(byteCodeInstraction.opCode.equals( "39" ))
            {
                constantValueOne = DOUBLE_ONE;
            }
            
            
            handleIncreement( localVariable, varType, value, data, constantValueOne );
            
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, data );
            
            
        }
        else if ( byteCodeInstraction.opCode.equals( "12" ) || byteCodeInstraction.opCode.equals( "13" ) || byteCodeInstraction.opCode.equals( "14" ) )

        {
            int index = -1;
            if ( byteCodeInstraction.opCode.equals( "12" ) )
            {
                index = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            }
            else
            {
                int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
                int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
                index = ( firstHalf << 8 ) + secondHalf;
            }
            String data = "";
            if ( byteCodeParser.getConstantStringMap().get( index ) != null )
            {
                ConstantUtf8 dataUtf8 = byteCodeParser.getConstantUtf8Map().get( byteCodeParser.getConstantStringMap().get( index ).stringIndex );
                data = "\"" + dataUtf8.utf8String + "\"";
            }
            else if ( byteCodeParser.getConstantIntegerMap().get( index ) != null )
            {
                data = "" + byteCodeParser.getConstantIntegerMap().get( index ).bytes;
            }
            else if ( byteCodeParser.getConstantFloatMap().get( index ) != null )
            {
                data = "" + byteCodeParser.getConstantFloatMap().get( index ).bytes + "f";
            }
            else if ( byteCodeParser.getConstantLongMap().get( index ) != null )
            {
                data = "" + byteCodeParser.getConstantLongMap().get( index ).bytes + "L";
            }
            else if ( byteCodeParser.getConstantDoubleMap().get( index ) != null )
            {
                data = "" + byteCodeParser.getConstantDoubleMap().get( index ).bytes + "d";
            }
            methodStack.push( data );
        }
        else if ( byteCodeInstraction.opCode.equals( "10" ) )
        {
            int data = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            methodStack.push( "" + data );
        }
        else if ( byteCodeInstraction.opCode.equals( "11" ) )
        {
            int data = Integer.parseInt( byteCodeInstraction.operands[0] + byteCodeInstraction.operands[1], 16 );
            methodStack.push( "" + data );
        }
        else if ( byteCodeInstraction.opCode.equals( "B5" ) )
        {
            //putfield
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantFieldref constantFieldref = byteCodeParser.getConstantFieldrefMap().get( index );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantFieldref.name_and_type_index );
            String var = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            String varType = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.descriptor_index ).utf8String;
            String value = methodStack.pop();
            if ( varType.equals( "Z" ) )
            {
                if ( value.equals( "0" ) )
                {
                    value = "false";
                }
                else if ( value.equals( "1" ) )
                {
                    value = "true";
                }
            }
            String objRef = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( objRef ).append( "." ).append( var ).append( " = " ).append( value ).append( ";\n" ) ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "B3" ) )
        {
            //putstatic
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantFieldref constantFieldref = byteCodeParser.getConstantFieldrefMap().get( index );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantFieldref.name_and_type_index );
            String var = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            String varType = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.descriptor_index ).utf8String;
            boolean isBooleanType = varType.equals( "Z" );
            String value = methodStack.pop();
            if ( isBooleanType )
            {
                if ( value.equals( '0' ) )
                {
                    value = "false";
                }
                else if ( value.equals( '1' ) )
                {
                    value = "true";
                }
            }
            //String objRef = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( var ).append( " = " ).append( value ).append( ";\n" ) ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "B6" ) || byteCodeInstraction.opCode.equals( "B7" ) )
        {
            //invokespecial and invokevirtual

            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantMethodref constantMethodref = byteCodeParser.getConstantMethodrefMap().get( index );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantMethodref.name_and_type_index );
            String methodName = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            String methodDescriptor = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.descriptor_index ).utf8String;
            String argumentsStr = methodDescriptor.substring( methodDescriptor.indexOf( '(' ) + 1, methodDescriptor.lastIndexOf( ')' ) );
            String argVal = "";
            if ( !argumentsStr.equals( "" ) )
            {
                String[] args = argumentsStr.split( ";" );

                for ( String arg : args )
                {
                    if ( !argVal.equals( "" ) )
                        argVal += ", ";
                    argVal += methodStack.pop();
                }
            }
            StringBuilder methodCallData = new StringBuilder();
            String objRef = methodStack.pop();

            if ( methodName.equals( "<init>" ) && objRef.startsWith( "new " ) )
            {
                methodStack.pop();//just pop the extra entry from [new] opcode
                methodCallData.append( objRef ).append( "(" ).append( argVal ).append( ")" );
            }
            else if ( byteCodeInstraction.opCode.equals( "B7" ) && methodName.equals( "<init>" ) && objRef.equals( "this" ) )
            {
                methodCallData.append( "super" ).append( "(" ).append( argVal ).append( ")" );
            }
            else
            {
                methodCallData.append( objRef ).append( "." ).append( methodName ).append( "(" ).append( argVal ).append( ")" );
            }
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, methodCallData );
        }
        else if ( byteCodeInstraction.opCode.equals( "B8" ) )
        {
            //invokestatic
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantMethodref constantMethodref = byteCodeParser.getConstantMethodrefMap().get( index );
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( constantMethodref.classIndex );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantMethodref.name_and_type_index );
            String methodName = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            String methodDescriptor = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.descriptor_index ).utf8String;
            String argumentsStr = methodDescriptor.substring( methodDescriptor.indexOf( '(' ) + 1, methodDescriptor.lastIndexOf( ')' ) );
            String argVal = "";
            if ( !argumentsStr.equals( "" ) )
            {
                String[] args = argumentsStr.split( ";" );

                for ( String arg : args )
                {
                    if ( !argVal.equals( "" ) )
                        argVal += ", ";
                    argVal += methodStack.pop();
                }
            }
            StringBuilder methodCallData = new StringBuilder();
            if ( className.equals( bytecodeToJavaDecoder.packageAndClassName[1] ) )
            {
                methodCallData.append( methodName ).append( "(" ).append( argVal ).append( ")" );
            }
            else
            {
                methodCallData.append( className ).append( "." ).append( methodName ).append( "(" ).append( argVal ).append( ")" );
            }
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, methodCallData );
        }
        else if ( byteCodeInstraction.opCode.equals( "B2" ) )
        {
            //getstatic
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantFieldref constantFieldref = byteCodeParser.getConstantFieldrefMap().get( index );
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( constantFieldref.classIndex );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantFieldref.name_and_type_index );
            String var = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            methodStack.push( className + "." + var );
        }
        else if ( byteCodeInstraction.opCode.equals( "B4" ) )
        {
            //getfield
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantFieldref constantFieldref = byteCodeParser.getConstantFieldrefMap().get( index );
            ConstantNameAndType constantNameAndType = byteCodeParser.getConstantNameAndTypeMap().get( constantFieldref.name_and_type_index );
            String var = byteCodeParser.getConstantUtf8Map().get( constantNameAndType.nameIndex ).utf8String;
            String objRef = methodStack.pop();
            methodStack.push( objRef + "." + var );
        }
        else if ( byteCodeInstraction.opCode.equals( "BB" ) )
        {
            //new
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( index );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            methodStack.push( "new " + className );
        }
        else if ( byteCodeInstraction.opCode.equals( "59" ) )
        {
            methodStack.push( methodStack.peek() );
        }
        else if ( byteCodeInstraction.opCode.equals( "60" ) || byteCodeInstraction.opCode.equals( "61" ) || byteCodeInstraction.opCode.equals( "62" ) || byteCodeInstraction.opCode.equals( "63" ) )
        {
            //ADD
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " + " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "64" ) || byteCodeInstraction.opCode.equals( "65" ) || byteCodeInstraction.opCode.equals( "66" ) || byteCodeInstraction.opCode.equals( "67" ) )
        {
            //SUB
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " - " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "68" ) || byteCodeInstraction.opCode.equals( "69" ) || byteCodeInstraction.opCode.equals( "6A" ) || byteCodeInstraction.opCode.equals( "6B" ) )
        {
            //MUL
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " * " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "6C" ) || byteCodeInstraction.opCode.equals( "6D" ) || byteCodeInstraction.opCode.equals( "6E" ) || byteCodeInstraction.opCode.equals( "6F" ) )
        {
            //DIV
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " / " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "70" ) || byteCodeInstraction.opCode.equals( "71" ) || byteCodeInstraction.opCode.equals( "72" ) || byteCodeInstraction.opCode.equals( "73" ) )
        {
            //REM
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " % " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "74" ) || byteCodeInstraction.opCode.equals( "75" ) || byteCodeInstraction.opCode.equals( "76" ) || byteCodeInstraction.opCode.equals( "77" ) )
        {
            //NEG
            String data = methodStack.pop();
            methodStack.push( "-(" + data + ")" );
        }
        else if ( byteCodeInstraction.opCode.equals( "7E" ) || byteCodeInstraction.opCode.equals( "7F" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " & " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "80" ) || byteCodeInstraction.opCode.equals( "81" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " | " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "78" ) || byteCodeInstraction.opCode.equals( "79" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " << " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "7A" ) || byteCodeInstraction.opCode.equals( "7B" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " >> " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "7C" ) || byteCodeInstraction.opCode.equals( "7D" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " >> " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "82" ) || byteCodeInstraction.opCode.equals( "83" ) )
        {
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( "( " + data2 + " ^ " + data1 + " )" );
        }
        else if ( byteCodeInstraction.opCode.equals( "84" ) )
        {
            //IINC
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            LocalVariable localVar = getLocalVariable( methodDetail, firstHalf, byteCodeInstraction.startPCIndex );

            String localVariable = getNameFromNameIndex( localVar.name_index );

            String varType = "";
            if ( !localVar.isDeclared )
            {
                localVar.isDeclared = true;
                varType = byteCodeParser.getConstantUtf8Map().get( localVar.descriptor_index ).utf8String;
                varType = bytecodeToJavaDecoder.getFormatedFieldType( varType );
            }
            StringBuilder data = new StringBuilder();
            if( secondHalf == 1 )
            {
                if(!methodStack.isEmpty())
                {
                   String topData = methodStack.peek();
                   if(topData.equals( localVariable ))
                   {
                       methodStack.pop();
                       data = new StringBuilder().append( "".equals( varType ) ? varType : ( varType + " " ) ).append( localVariable ).append( "++" ) ;
                   }
                   else
                   {
                       data = new StringBuilder().append( "".equals( varType ) ? varType : ( varType + " " ) ).append( "++").append( localVariable )  ;
                   }
                }
                else
                {
                    data = new StringBuilder().append( "".equals( varType ) ? varType : ( varType + " " ) ).append( "++").append( localVariable )  ;
                }
            }
            else
            {
                data = new StringBuilder().append( "".equals( varType ) ? varType : ( varType + " " ) ).append( localVariable ).append( " = " ).append( localVariable ).append( " + " ).append( secondHalf ) ;
            }
        
            finalizeInstruction( createLine, startPCIndexMethodCodeMap, startPCIndex, data );

        }
        else if ( byteCodeInstraction.opCode.equals( "C0" ) )
        {
            //CHECKCAST
            String data = methodStack.pop();
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( index );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            methodStack.push( "(" + className + ") " + data );
        }
        else if ( byteCodeInstraction.opCode.equals( "B0" ) || byteCodeInstraction.opCode.equals( "AC" ) || byteCodeInstraction.opCode.equals( "AD" ) || byteCodeInstraction.opCode.equals( "AE" ) || byteCodeInstraction.opCode.equals( "AF" ) )
        {
            //ARETURN, DRETURN, FRETURN, IRETURN, LRETURN
            String data = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "return " ).append( data ).append( ";\n" ) ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "02" ) || byteCodeInstraction.opCode.equals( "03" ) || byteCodeInstraction.opCode.equals( "04" ) || byteCodeInstraction.opCode.equals( "05" ) || byteCodeInstraction.opCode.equals( "06" ) || byteCodeInstraction.opCode.equals( "07" ) || byteCodeInstraction.opCode.equals( "08" ) )
        {
            //ICONST_M1,ICONST_0,....
            int digit = Integer.parseInt( byteCodeInstraction.opCode.substring( 1 ) );
            int result = digit - 3;
            methodStack.push( "" + result );
        }
        else if ( byteCodeInstraction.opCode.equals( "0E" ) )
        {
            //DCONST_0
            methodStack.push( "0.0d" );
        }
        else if ( byteCodeInstraction.opCode.equals( "0F" ) )
        {
            //DCONST_1
            methodStack.push( DOUBLE_ONE );
        }
        else if ( byteCodeInstraction.opCode.equals( "0B" ) )
        {
            //FCONST_0
            methodStack.push( "0.0f" );
        }
        else if ( byteCodeInstraction.opCode.equals( "0C" ) )
        {
            //FCONST_1
            methodStack.push( FLOAT_ONE );
        }
        else if ( byteCodeInstraction.opCode.equals( "0D" ) )
        {
            //FCONST_2
            methodStack.push( "2.0f" );
        }
        else if ( byteCodeInstraction.opCode.equals( "09" ) )
        {
            //lCONST_0
            methodStack.push( "0" );
        }
        else if ( byteCodeInstraction.opCode.equals( "0A" ) )
        {
            //LCONST_1
            methodStack.push( "1" );
        }
        else if ( byteCodeInstraction.opCode.equals( "90" ) || byteCodeInstraction.opCode.equals( "86" ) || byteCodeInstraction.opCode.equals( "89" ) )
        {
            methodStack.push( "( float )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "8E" ) || byteCodeInstraction.opCode.equals( "8B" ) || byteCodeInstraction.opCode.equals( "88" ) )
        {
            methodStack.push( "( int )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "8F" ) || byteCodeInstraction.opCode.equals( "8C" ) || byteCodeInstraction.opCode.equals( "85" ) )
        {
            methodStack.push( "( long )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "8D" ) || byteCodeInstraction.opCode.equals( "87" ) || byteCodeInstraction.opCode.equals( "8A" ) )
        {
            methodStack.push( "( double )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "91" ) )
        {
            methodStack.push( "( byte )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "92" ) )
        {
            methodStack.push( "( char )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "93" ) )
        {
            methodStack.push( "( short )" + methodStack.pop() );
        }
        else if ( byteCodeInstraction.opCode.equals( "01" ) )
        {
            methodStack.push( "null" );
        }
        else if ( byteCodeInstraction.opCode.equals( "BD" ) )
        {
            //anewarray
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( index );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            className = bytecodeToJavaDecoder.extractClassAndPackage( className );
            methodStack.push( "new " + className + "[" + methodStack.pop() + "]" );
        }
        else if ( byteCodeInstraction.opCode.equals( "BC" ) )
        {
            //newarray
            int atype = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            methodStack.push( "new " + DecodeHelper.getAType( atype ) + "[" + methodStack.pop() + "]" );
        }
        else if ( byteCodeInstraction.opCode.equals( "C5" ) )
        {
            //multinewarray
            int firstHalf = Integer.parseInt( byteCodeInstraction.operands[0], 16 );
            int secondHalf = Integer.parseInt( byteCodeInstraction.operands[1], 16 );
            int index = ( firstHalf << 8 ) + secondHalf;
            ConstantClass constantClass = byteCodeParser.getConstantClassMap().get( index );
            String className = byteCodeParser.getConstantUtf8Map().get( constantClass.nameIndex ).utf8String;
            String typeName = bytecodeToJavaDecoder.getFormatedFieldType( className, false );
            //className = extractClassAndPackage( className );
            int dimensions = Integer.parseInt( byteCodeInstraction.operands[2], 16 );
            String dim = "";
            for ( int count = 0; count < dimensions; count++ )
            {
                dim = "[" + methodStack.pop() + "]" + dim;
            }

            methodStack.push( "new " + typeName + dim );

        }
        else if ( byteCodeInstraction.opCode.equals( "BE" ) )
        {
            //arraylength
            methodStack.push( methodStack.pop() + ".length" );
        }
        else if ( byteCodeInstraction.opCode.equals( "2E" ) || byteCodeInstraction.opCode.equals( "2F" ) || byteCodeInstraction.opCode.equals( "35" ) || byteCodeInstraction.opCode.equals( "32" ) || byteCodeInstraction.opCode.equals( "33" ) || byteCodeInstraction.opCode.equals( "34" ) || byteCodeInstraction.opCode.equals( "31" ) || byteCodeInstraction.opCode.equals( "30" ) )
        {
            //aaload, iaload, laload, etc...
            String index = methodStack.pop();
            String arrayRef = methodStack.pop();
            methodStack.push( arrayRef + "[" + index + "]" );
        }
        else if ( byteCodeInstraction.opCode.equals( "53" ) || byteCodeInstraction.opCode.equals( "54" ) || byteCodeInstraction.opCode.equals( "55" ) || byteCodeInstraction.opCode.equals( "52" ) || byteCodeInstraction.opCode.equals( "51" ) || byteCodeInstraction.opCode.equals( "4F" ) || byteCodeInstraction.opCode.equals( "50" ) || byteCodeInstraction.opCode.equals( "56" ) )
        {
            //aastore, iastore, lastore, etc...
            String value = methodStack.pop();
            String index = methodStack.pop();
            String arrayRef = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( arrayRef ).append( "[" ).append( index ).append( "]" ).append( " = " ).append( value ).append( ";\n" ) ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "5F" ) )
        {
            //swap
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( data1 );
            methodStack.push( data2 );
        }
        else if ( byteCodeInstraction.opCode.equals( "5A" ) )
        {
            //dup_x1
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            methodStack.push( data1 );
            methodStack.push( data2 );
            methodStack.push( data1 );
        }
        else if ( byteCodeInstraction.opCode.equals( "5B" ) )
        {
            //dup_x2
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            String data3 = methodStack.pop();
            methodStack.push( data1 );
            methodStack.push( data3 );
            methodStack.push( data2 );
            methodStack.push( data1 );
        }
        else if ( byteCodeInstraction.opCode.equals( "5C" ) )
        {
            //dup2 //TODO Need to look in to the duplicating stack logic
            String data1 = methodStack.pop();
            //String data2 = methodStack.pop();
            //methodStack.push( data2 );
            methodStack.push( data1 );
            //methodStack.push( data2 );
            methodStack.push( data1 );
        }
        else if ( byteCodeInstraction.opCode.equals( "5D" ) )
        {
            //dup2_x1
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            String data3 = methodStack.pop();
            methodStack.push( data2 );
            methodStack.push( data1 );
            methodStack.push( data3 );
            methodStack.push( data2 );
            methodStack.push( data1 );
        }
        else if ( byteCodeInstraction.opCode.equals( "5E" ) )
        {
            //dup2_x2
            String data1 = methodStack.pop();
            String data2 = methodStack.pop();
            String data3 = methodStack.pop();
            String data4 = methodStack.pop();
            methodStack.push( data2 );
            methodStack.push( data1 );
            methodStack.push( data4 );
            methodStack.push( data3 );
            methodStack.push( data2 );
            methodStack.push( data1 );
        }
        else if ( byteCodeInstraction.opCode.equals( "C2" ) )
        {
            //monitorenter
            String objRef = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "synchronized ( " ).append( objRef ).append( " )" ).append( "{" ), true, false ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "C3" ) )
        {
            //monitorenter
            String objRef = methodStack.pop();
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "}" ).append( "\n" ), false, true ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A1" ) || byteCodeInstraction.opCode.equals( "A2" ) || byteCodeInstraction.opCode.equals( "A3" ) || byteCodeInstraction.opCode.equals( "A4" ) || byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "A6" ) || byteCodeInstraction.opCode.equals( "9F" ) )
        {
            String value2 = methodStack.pop();
            String value1 = methodStack.pop();
            String symbol = "";
            String logicalExp = "";

            if ( !isConditionalStarted )
            {
                //CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.length + 1 ); TODO don't know why we had this way
                CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.length );
                if ( codeLineNumber != null )
                {
                    handleConditionalType2( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex, value1, value2 );
                }
                else
                {
                    isConditionalStarted = true;
                    conditionalType = getConditionalType( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex );
                    if ( !conditionalType.equals( "if" ) )
                    {
                        if ( byteCodeInstraction.branchOffset < 0 )
                        {
                            if ( byteCodeInstraction.opCode.equals( "A1" ) )
                            {
                                //icmplt
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                            {
                                //icmpgt
                                symbol = " > ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                            {

                                symbol = " == ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                            {
                                symbol = " != ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                            {
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                            {
                                symbol = " <= ";
                            }
                            logicalExp = " || ";
                        }
                        else
                        {
                            if ( byteCodeInstraction.opCode.equals( "A1" ) )
                            {
                                //icmplt
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                            {
                                //icmpgt
                                symbol = " <= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                            {

                                symbol = " != ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                            {
                                symbol = " == ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                            {
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                            {
                                symbol = " > ";
                            }
                            logicalExp = " && ";
                        }
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                        if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                        {
                            if ( methodCodeLineList != null )
                            {
                                methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "while( " ).append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp ) ) );
                                methodCodeLineList.add( 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );

                            }
                            methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "}\n" ), false, true ) );

                        }
                        else
                        {
                            if ( methodCodeLineList != null )
                            {
                                int index = 0;
                                if ( methodCodeLineList.get( 0 ).isCurlyBracketEnded )
                                {
                                    index++;
                                }
                                methodCodeLineList.add( index, new MethodCodeLine( new StringBuilder( "do\n" ) ) );
                                methodCodeLineList.add( index + 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                            }
                            methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                            methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}while( " ).append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp ), false, true ) );
                        }

                    }
                    else
                    {
                        //IF****************************************
                        codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( startPCIndex );
                        if ( ( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset < codeLineNumber.end_pc ) || conditionalJumpedByteCodeInst.startPCIndex == byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset )
                        {
                            if ( byteCodeInstraction.opCode.equals( "A1" ) )
                            {
                                //icmplt
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                            {
                                //icmpgt
                                symbol = " <= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                            {

                                symbol = " != ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                            {
                                symbol = " == ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                            {
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                            {
                                symbol = " > ";
                            }
                            logicalExp = " && ";
                        }
                        else
                        {
                            if ( byteCodeInstraction.opCode.equals( "A1" ) )
                            {
                                //icmplt
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                            {
                                //icmpgt
                                symbol = " > ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                            {

                                symbol = " == ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                            {
                                symbol = " != ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                            {
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                            {
                                symbol = " <= ";
                            }
                            logicalExp = " || ";
                        }
                        processIfConditionalStart( methodDetail, startPCIndexMethodCodeMap, startPCIndex, value2, value1, symbol, logicalExp );
                    }

                }
            }
            else
            {
                CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( startPCIndex );
                if ( !conditionalType.equals( "if" ) )
                {
                    if ( byteCodeInstraction.branchOffset < 0 )
                    {
                        if ( byteCodeInstraction.opCode.equals( "A1" ) )
                        {
                            //icmplt
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                        {
                            //icmpgt
                            symbol = " > ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                        {

                            symbol = " == ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                        {
                            symbol = " != ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                        {
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                        {
                            symbol = " <= ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " || ";
                        }
                    }
                    else
                    {
                        if ( byteCodeInstraction.opCode.equals( "A1" ) )
                        {
                            //icmplt
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                        {
                            //icmpgt
                            symbol = " <= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                        {

                            symbol = " != ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                        {
                            symbol = " == ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                        {
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                        {
                            symbol = " > ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " && ";
                        }
                    }

                    if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                    {
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                        if ( methodCodeLineList != null )
                        {
                            methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                        }
                    }
                    else
                    {
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                        methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                    }

                    if ( codeLineNumber.end_pc == byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                    {
                        if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                        {
                            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                            methodCodeLineList.get( 0 ).codeLine.append( " )\n" );
                        }
                        else
                        {
                            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                            methodCodeLineList.get( 0 ).codeLine.append( " );\n" );
                        }
                        conditionalType = "";
                        isConditionalStarted = false;
                        conditionalPrevByteCodeInst = null;
                        conditionalJumpedByteCodeInst = null;
                    }
                }
                else
                {

                    if ( ( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset < codeLineNumber.end_pc ) || conditionalJumpedByteCodeInst.startPCIndex == byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset )
                    {
                        if ( byteCodeInstraction.opCode.equals( "A1" ) )
                        {
                            //icmplt
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                        {
                            //icmpgt
                            symbol = " <= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                        {

                            symbol = " != ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                        {
                            symbol = " == ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                        {
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                        {
                            symbol = " > ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " && ";
                        }
                    }
                    else
                    {
                        if ( byteCodeInstraction.opCode.equals( "A1" ) )
                        {
                            //icmplt
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A3" ) )
                        {
                            //icmpgt
                            symbol = " > ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
                        {

                            symbol = " == ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
                        {
                            symbol = " != ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A2" ) )
                        {
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "A4" ) )
                        {
                            symbol = " <= ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " || ";
                        }
                    }

                    List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                    methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                    if ( codeLineNumber.end_pc == byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                    {
                        methodCodeLineList.get( 0 ).codeLine.append( " )\n" );
                        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                        conditionalType = "";
                        isConditionalStarted = false;
                        conditionalPrevByteCodeInst = null;
                        conditionalJumpedByteCodeInst = null;
                    }
                }

            }
            // handleConditionalType2( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex );

        }
        else if ( byteCodeInstraction.opCode.equals( "94" ) || byteCodeInstraction.opCode.equals( "95" ) || byteCodeInstraction.opCode.equals( "96" ) || byteCodeInstraction.opCode.equals( "97" ) || byteCodeInstraction.opCode.equals( "98" ) )
        {
            isCompare = true;
        }
        else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "9B" ) || byteCodeInstraction.opCode.equals( "9C" ) || byteCodeInstraction.opCode.equals( "9D" ) || byteCodeInstraction.opCode.equals( "9E" ) )
        {
            String value2 = "0";
            if ( isCompare )
            {
                value2 = methodStack.pop();
                isCompare =  false;
            }
            String value1 = methodStack.pop();
            String symbol = "";
            String logicalExp = "";

            if ( !isConditionalStarted )
            {
                //CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.length + 1 ); TODO don't know why we had this way
                CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.length );
                if ( codeLineNumber != null )
                {
                    handleConditionalType1( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex, value1, value2 );
                }
                else
                {
                    isConditionalStarted = true;
                    conditionalType = getConditionalType( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex );
                    if ( !conditionalType.equals( "if" ) )
                    {
                        if ( byteCodeInstraction.branchOffset < 0 )
                        {
                            if ( byteCodeInstraction.opCode.equals( "9B" ) )
                            {
                                //icmplt
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                            {
                                //icmpgt
                                symbol = " > ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                symbol = " == ";
                                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                symbol = " != ";
                                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                            {
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                            {
                                symbol = " <= ";
                            }
                            logicalExp = " || ";
                        }
                        else
                        {
                            if ( byteCodeInstraction.opCode.equals( "9B" ) )
                            {
                                //icmplt
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                            {
                                //icmpgt
                                symbol = " <= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                symbol = " != ";
                                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                symbol = " == ";
                                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                            {
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                            {
                                symbol = " > ";
                            }
                            logicalExp = " && ";
                        }
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                        if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                        {
                            if ( methodCodeLineList != null )
                            {
                                methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "while( " ).append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp ) ) );
                                methodCodeLineList.add( 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                            }
                            methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                            methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "}\n" ), false, true ) );
                        }
                        else
                        {
                            if ( methodCodeLineList != null )
                            {
                                int index = 0;
                                if ( methodCodeLineList.get( 0 ).isCurlyBracketEnded )
                                {
                                    index++;
                                }
                                methodCodeLineList.add( index, new MethodCodeLine( new StringBuilder( "do\n" ) ) );
                                methodCodeLineList.add( index + 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                            }
                            methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                            methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}while( " ).append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp ), false, true ) );
                        }
                    }
                    else
                    {
                        //IF****************************************
                        codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( startPCIndex );
                        if ( ( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset < codeLineNumber.end_pc ) || conditionalJumpedByteCodeInst.startPCIndex == byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset )
                        {
                            if ( byteCodeInstraction.opCode.equals( "9B" ) )
                            {
                                //icmplt
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                            {
                                //icmpgt
                                symbol = " <= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                symbol = " != ";
                                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                symbol = " == ";
                                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                            {
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                            {
                                symbol = " > ";
                            }

                            logicalExp = " && ";
                        }
                        else
                        {
                            if ( byteCodeInstraction.opCode.equals( "9B" ) )
                            {
                                //icmplt
                                symbol = " < ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                            {
                                //icmpgt
                                symbol = " > ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                symbol = " == ";
                                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                symbol = " != ";
                                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                                {
                                    value2 = "null";
                                }
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                            {
                                symbol = " >= ";
                            }
                            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                            {
                                symbol = " <= ";
                            }

                            logicalExp = " || ";

                        }

                        processIfConditionalStart( methodDetail, startPCIndexMethodCodeMap, startPCIndex, value2, value1, symbol, logicalExp );

                    }

                }
            }
            else
            {
                CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( startPCIndex );
                if ( !conditionalType.equals( "if" ) )
                {
                    if ( byteCodeInstraction.branchOffset < 0 )
                    {
                        if ( byteCodeInstraction.opCode.equals( "9B" ) )
                        {
                            //icmplt
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                        {
                            //icmpgt
                            symbol = " > ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                        {
                            symbol = " == ";
                            if ( byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                        {
                            symbol = " != ";
                            if ( byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                        {
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                        {
                            symbol = " <= ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " || ";
                        }
                    }
                    else
                    {
                        if ( byteCodeInstraction.opCode.equals( "9B" ) )
                        {
                            //icmplt
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                        {
                            //icmpgt
                            symbol = " <= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                        {
                            symbol = " != ";
                            if ( byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                        {
                            symbol = " == ";
                            if ( byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                        {
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                        {
                            symbol = " > ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " && ";
                        }
                    }

                    if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                    {
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                        if ( methodCodeLineList != null )
                        {
                            methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                        }
                    }
                    else
                    {
                        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                        methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                    }

                    if ( codeLineNumber.end_pc == byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                    {
                        if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                        {
                            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( conditionalJumpedByteCodeInst.startPCIndex );
                            methodCodeLineList.get( 0 ).codeLine.append( " )\n" );
                        }
                        else
                        {
                            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                            methodCodeLineList.get( 0 ).codeLine.append( " );\n" );
                        }
                        conditionalType = "";
                        isConditionalStarted = false;
                        conditionalPrevByteCodeInst = null;
                        conditionalJumpedByteCodeInst = null;
                    }
                }
                else
                {
                    if ( ( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset < codeLineNumber.end_pc ) || conditionalJumpedByteCodeInst.startPCIndex == byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset )
                    {

                        if ( byteCodeInstraction.opCode.equals( "9B" ) )
                        {
                            //icmplt
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                        {
                            //icmpgt
                            symbol = " <= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                        {
                            symbol = " != ";
                            if ( byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                        {
                            symbol = " == ";
                            if ( byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                        {
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                        {
                            symbol = " > ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " && ";
                        }

                    }
                    else
                    {
                        if ( byteCodeInstraction.opCode.equals( "9B" ) )
                        {
                            //icmplt
                            symbol = " < ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9D" ) )
                        {
                            //icmpgt
                            symbol = " > ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
                        {
                            symbol = " == ";
                            if ( byteCodeInstraction.opCode.equals( "C6" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
                        {
                            symbol = " != ";
                            if ( byteCodeInstraction.opCode.equals( "C7" ) )
                            {
                                value2 = "null";
                            }
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9C" ) )
                        {
                            symbol = " >= ";
                        }
                        else if ( byteCodeInstraction.opCode.equals( "9E" ) )
                        {
                            symbol = " <= ";
                        }
                        if ( codeLineNumber.end_pc != byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                        {
                            logicalExp = " || ";
                        }
                    }

                    List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
                    methodCodeLineList.get( 0 ).codeLine.append( value1 ).append( symbol ).append( value2 ).append( " " ).append( logicalExp );
                    if ( codeLineNumber.end_pc == byteCodeInstraction.startPCIndex + byteCodeInstraction.length - 1 )
                    {
                        methodCodeLineList.get( 0 ).codeLine.append( " )\n" );
                        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                        conditionalType = "";
                        isConditionalStarted = false;
                        conditionalPrevByteCodeInst = null;
                        conditionalJumpedByteCodeInst = null;
                    }
                }

            }

            // handleConditionalType1( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex );

        }
        else if ( byteCodeInstraction.opCode.equals( "BF" ) )
        {
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "throw " ).append( methodStack.pop() ).append( ";\n" ), false, false ) );

        }
        else if ( byteCodeInstraction.opCode.equals( "A7" ) )
        {
            if ( methodDetail.tryEndStartPcSet.contains( byteCodeInstraction.startPCIndex ) )
            {
                short byte1 = Short.parseShort( byteCodeInstraction.operands[0], 16 );
                short byte2 = Short.parseShort( byteCodeInstraction.operands[1], 16 );
                short branch = (short)( ( byte1 << (short)8 ) + byte2 );
                List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( byteCodeInstraction.startPCIndex + branch, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
            }

            if ( startPCIndex != byteCodeInstraction.startPCIndex )
            {
                if ( !methodStack.isEmpty() )
                {
                    String data = methodStack.pop();
                    if ( data != null )
                    {
                        List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( data ).append( ";\n" ), false, false ) );
                    }
                }
            }
            else
            {

                ByteCodeInstraction jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );

                if ( jumpedByteCodeInst.isLoop )
                {
                    List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                    methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "continue;\n" ), false, false ) );
                }
                else
                {
                    ByteCodeInstraction nextByteCodeInst = jumpedByteCodeInst;
                    CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( jumpedByteCodeInst.startPCIndex );
                    while ( nextByteCodeInst.startPCIndex < codeLineNumber.end_pc )
                    {
                        if ( isConditionalBranching( nextByteCodeInst.opCode ) && nextByteCodeInst.branchOffset < 0 )
                        {
                            jumpedByteCodeInst.isLoop = true;
                            break;
                        }
                        nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
                    }

                    if ( !jumpedByteCodeInst.isLoop )
                    {
                        ByteCodeInstraction prevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( jumpedByteCodeInst.previousInstructionStartPCIndex );
                        if ( isConditionalBranching( prevByteCodeInst.opCode ) && prevByteCodeInst.branchOffset < 0 )
                        {
                            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                            methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "break;\n" ), false, false ) );
                        }
                    }
                }
            }
        }
    }

    private void handleIncreement( String localVariable, String varType, String expression, StringBuilder data, String value )
    {
        if( expression.equals( "( "+localVariable +" + "+value+" )" ) ||  expression.equals( "( byte )( "+localVariable +" + "+value+" )" ) ||  expression.equals( "( short )( "+localVariable +" + "+value+" )" ))
        {
            if(!methodStack.isEmpty() &&  methodStack.peek().equals( expression ) )
            {
                    methodStack.pop();
                    data.append( "++" + localVariable   );    
            }
            else 
            {
                if(methodStack.peek().equals( localVariable ))
                {
                    methodStack.pop(); 
                }
                data.append( localVariable + "++" );
            }
        }
        else
        {
            data.append( "".equals( varType ) ? varType : ( varType + " " ) ).append( localVariable ).append( " = " ).append( expression ) ;    
        }
    }

    private void finalizeInstruction( boolean createLine, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex, StringBuilder data )
    {
        if(createLine)
        {
            List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
            methodCodeLineList.add( new MethodCodeLine(data.append( ";\n" )) );
            methodStack.clear();
        }
        else
        {
            methodStack.push( data.toString() );
        }
    }

    private boolean isLocalVarPreIncreement( String localVariable )
    {
        if(!methodStack.isEmpty())
        {
            if( methodStack.lastElement().equals("++"+ localVariable ))
            {
                return true;
            }
        }
        return false;
    }

    private void processIfConditionalStart( MethodDetail methodDetail, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex, String value2, String value1, String symbol, String logicalExp )
    {
        CodeLineNumber codeLineNo = methodDetail.startPcCodeLineNumberMap.get( conditionalJumpedByteCodeInst.startPCIndex );
        List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "if( " ).append( value1 ).append( symbol ).append( value2 ).append( logicalExp ) ) );
        boolean isJumpedStmtConditional = false;
        ByteCodeInstraction nextByteCodeInst = conditionalJumpedByteCodeInst;
        if ( codeLineNo != null )
        {
            while ( nextByteCodeInst.startPCIndex < codeLineNo.end_pc )
            {
                if ( isConditionalBranching( nextByteCodeInst.opCode ) )
                {
                    isJumpedStmtConditional = true;
                    if ( nextByteCodeInst.branchOffset < 0 )
                    {
                        break;
                    }
                }
                nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
            }
        }

        List< MethodCodeLine > prevMethodCodeLineList = getMethodCodeLineList( conditionalJumpedByteCodeInst.previousInstructionStartPCIndex, startPCIndexMethodCodeMap );

        if ( isJumpedStmtConditional && nextByteCodeInst.branchOffset < 0 )
        {
            ByteCodeInstraction jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + nextByteCodeInst.branchOffset );
            conditionalPrevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( jumpedByteCodeInst.previousInstructionStartPCIndex );
            prevMethodCodeLineList = getMethodCodeLineList( conditionalPrevByteCodeInst.startPCIndex, startPCIndexMethodCodeMap );
        }

        if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
        {
            if ( isJumpedStmtConditional )
            {
                prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
                if ( !methodDetail.startPcCodeLineNumberMap.containsKey( conditionalPrevByteCodeInst.startPCIndex ) )
                {
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "else\n" ), false, false ) );
                }
            }
            else
            {
                prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
                if ( !methodDetail.startPcCodeLineNumberMap.containsKey( conditionalPrevByteCodeInst.startPCIndex ) )
                {
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "else\n" ), false, false ) );
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                    codeLineNo = methodDetail.startPcCodeLineNumberMap.get( conditionalPrevByteCodeInst.startPCIndex + conditionalPrevByteCodeInst.branchOffset );
                    isJumpedStmtConditional = false;
                    nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( conditionalPrevByteCodeInst.startPCIndex + conditionalPrevByteCodeInst.branchOffset );
                    if ( codeLineNo != null )
                    {
                        while ( nextByteCodeInst.startPCIndex < codeLineNo.end_pc )
                        {
                            if ( isConditionalBranching( nextByteCodeInst.opCode ) )
                            {
                                isJumpedStmtConditional = true;
                                if ( nextByteCodeInst.branchOffset < 0 )
                                {
                                    break;
                                }
                            }
                            nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
                        }
                    }
                    List< MethodCodeLine > elseJumpMethodCodeLineList = getMethodCodeLineList( conditionalPrevByteCodeInst.startPCIndex + conditionalPrevByteCodeInst.branchOffset, startPCIndexMethodCodeMap );
                    if ( isJumpedStmtConditional && nextByteCodeInst.branchOffset < 0 )
                    {
                        nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + nextByteCodeInst.branchOffset );
                        elseJumpMethodCodeLineList = getMethodCodeLineList( nextByteCodeInst.previousInstructionStartPCIndex, startPCIndexMethodCodeMap );
                    }
                    elseJumpMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );

                }
            }
        }
        else
        {
            prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
        }
    }

    private String getConditionalType( MethodDetail methodDetail, ByteCodeInstraction byteCodeInstraction, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex )
    {
        String conditional = "if";
        CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( startPCIndex );
        conditionalJumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );

        ByteCodeInstraction nextByteCodeInst = byteCodeInstraction;
        if ( codeLineNumber != null )
        {
            while ( nextByteCodeInst.startPCIndex < codeLineNumber.end_pc )
            {
                conditionalJumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + nextByteCodeInst.branchOffset );
                conditionalPrevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( conditionalJumpedByteCodeInst.previousInstructionStartPCIndex );
                if ( isConditionalBranching( nextByteCodeInst.opCode ) && nextByteCodeInst.branchOffset < 0 )
                {
                    if ( conditionalPrevByteCodeInst.opCode.equals( "A7" ) )
                    {
                        conditional = "while";
                    }
                    else
                    {
                        conditional = "dowhile";
                    }

                    break;
                }
                nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
            }
        }
        return conditional;
    }

    private void handleConditionalType2( MethodDetail methodDetail, ByteCodeInstraction byteCodeInstraction, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex, String value1, String value2 )
    {
        String symbol = "";
        if ( byteCodeInstraction.branchOffset < 0 )
        {
            if ( byteCodeInstraction.opCode.equals( "A1" ) )
            {
                //icmplt
                symbol = " < ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
            {
                //icmpgt
                symbol = " > ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
            {

                symbol = " == ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
            {
                symbol = " != ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
            {
                symbol = " >= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
            {
                symbol = " <= ";
            }

            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );
            ByteCodeInstraction jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );
            ByteCodeInstraction prevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( jumpedByteCodeInst.previousInstructionStartPCIndex );
            if ( prevByteCodeInst.opCode.equals( "A7" ) )
            {
                if ( methodCodeLineList != null )
                {
                    methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "while( " ).append( value1 ).append( symbol ).append( value2 ).append( " )" ).append( " \n" ) ) );
                    methodCodeLineList.add( 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );

                }
                methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "}\n" ), false, true ) );

            }
            else
            {
                if ( methodCodeLineList != null )
                {
                    int index = 0;
                    if ( methodCodeLineList.get( 0 ).isCurlyBracketEnded )
                    {
                        index++;
                    }
                    methodCodeLineList.add( index, new MethodCodeLine( new StringBuilder( "do\n" ) ) );
                    methodCodeLineList.add( index + 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );

                }
                methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}while( " ).append( value1 ).append( symbol ).append( value2 ).append( " );" ).append( " \n" ), false, true ) );

            }

        }
        else
        {

            if ( byteCodeInstraction.opCode.equals( "A1" ) )
            {
                //icmplt
                symbol = " >= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A3" ) )
            {
                //icmpgt
                symbol = " <= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A5" ) || byteCodeInstraction.opCode.equals( "9F" ) )
            {

                symbol = " != ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A0" ) || byteCodeInstraction.opCode.equals( "A6" ) )
            {
                symbol = " == ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A2" ) )
            {
                symbol = " < ";
            }
            else if ( byteCodeInstraction.opCode.equals( "A4" ) )
            {
                symbol = " > ";
            }

            processIfCondition( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex, value1, value2, symbol );

        }
    }

    private void processIfCondition( MethodDetail methodDetail, ByteCodeInstraction byteCodeInstraction, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex, String value1, String value2, String symbol )
    {
        ByteCodeInstraction jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );
        CodeLineNumber codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( jumpedByteCodeInst.startPCIndex );

        List< MethodCodeLine > methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "if( " ).append( value1 ).append( symbol ).append( value2 ).append( " )" ).append( " \n" ) ) );
        methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );

        boolean isJumpedStmtConditional = false;
        ByteCodeInstraction nextByteCodeInst = jumpedByteCodeInst;
        if ( codeLineNumber != null )
        {
            while ( nextByteCodeInst.startPCIndex < codeLineNumber.end_pc )
            {
                if ( isConditionalBranching( nextByteCodeInst.opCode ) )
                {
                    isJumpedStmtConditional = true;
                    if ( nextByteCodeInst.branchOffset < 0 )
                    {
                        break;
                    }
                }
                nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
            }
        }

        if ( isJumpedStmtConditional && nextByteCodeInst.branchOffset < 0 )
        {
            jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + nextByteCodeInst.branchOffset );
        }
        List< MethodCodeLine > prevMethodCodeLineList = getMethodCodeLineList( jumpedByteCodeInst.previousInstructionStartPCIndex, startPCIndexMethodCodeMap );
        ByteCodeInstraction prevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( jumpedByteCodeInst.previousInstructionStartPCIndex );
        if ( prevByteCodeInst.opCode.equals( "A7" ) )
        {
            if ( isJumpedStmtConditional )
            {
                prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
                if ( !methodDetail.startPcCodeLineNumberMap.containsKey( prevByteCodeInst.startPCIndex ) )
                {
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "else\n" ), false, false ) );
                }
            }
            else
            {
                prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
                if ( !methodDetail.startPcCodeLineNumberMap.containsKey( prevByteCodeInst.startPCIndex ) )
                {
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "else\n" ), false, false ) );
                    prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                    codeLineNumber = methodDetail.startPcCodeLineNumberMap.get( prevByteCodeInst.startPCIndex + prevByteCodeInst.branchOffset );
                    isJumpedStmtConditional = false;
                    nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( prevByteCodeInst.startPCIndex + prevByteCodeInst.branchOffset );
                    if ( codeLineNumber != null )
                    {
                        while ( nextByteCodeInst.startPCIndex < codeLineNumber.end_pc )
                        {
                            if ( isConditionalBranching( nextByteCodeInst.opCode ) )
                            {
                                isJumpedStmtConditional = true;
                                if ( nextByteCodeInst.branchOffset < 0 )
                                {
                                    break;
                                }
                            }
                            nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + ( nextByteCodeInst.length ) );
                        }
                    }
                    List< MethodCodeLine > elseJumpMethodCodeLineList = getMethodCodeLineList( prevByteCodeInst.startPCIndex + prevByteCodeInst.branchOffset, startPCIndexMethodCodeMap );
                    if ( isJumpedStmtConditional && nextByteCodeInst.branchOffset < 0 )
                    {
                        nextByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( nextByteCodeInst.startPCIndex + nextByteCodeInst.branchOffset );
                        elseJumpMethodCodeLineList = getMethodCodeLineList( nextByteCodeInst.previousInstructionStartPCIndex, startPCIndexMethodCodeMap );
                    }
                    elseJumpMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
                }
            }
        }
        else
        {
            prevMethodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}\n" ), false, true ) );
        }
    }

    private void handleConditionalType1( MethodDetail methodDetail, ByteCodeInstraction byteCodeInstraction, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap, int startPCIndex, String value1, String value2 )
    {
        String symbol = "";
        if ( byteCodeInstraction.branchOffset < 0 )
        {

            if ( byteCodeInstraction.opCode.equals( "9B" ) )
            {
                //icmplt
                symbol = " < ";
            }
            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
            {
                //icmpgt
                symbol = " > ";
            }
            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
            {
                symbol = " == ";
                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                {
                    value2 = "null";
                }
            }
            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
            {
                symbol = " != ";
                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                {
                    value2 = "null";
                }
            }
            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
            {
                symbol = " >= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
            {
                symbol = " <= ";
            }

            StringBuilder sb = null;
            List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );
            ByteCodeInstraction jumpedByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( byteCodeInstraction.startPCIndex + byteCodeInstraction.branchOffset );
            ByteCodeInstraction prevByteCodeInst = methodDetail.startPcByteCodeInstractionMap.get( jumpedByteCodeInst.previousInstructionStartPCIndex );
            if ( prevByteCodeInst.opCode.equals( "A7" ) )
            {
                if ( methodCodeLineList != null )
                {
                    methodCodeLineList.add( 0, new MethodCodeLine( new StringBuilder( "while( " ).append( value1 ).append( symbol ).append( value2 ).append( " )" ).append( " \n" ) ) );
                    methodCodeLineList.add( 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                }
                methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder().append( "}\n" ), false, true ) );
            }
            else
            {
                if ( methodCodeLineList != null )
                {
                    int index = 0;
                    if ( methodCodeLineList.get( 0 ).isCurlyBracketEnded )
                    {
                        index++;
                    }
                    methodCodeLineList.add( index, new MethodCodeLine( new StringBuilder( "do\n" ) ) );
                    methodCodeLineList.add( index + 1, new MethodCodeLine( new StringBuilder( "{\n" ), true, false ) );
                }
                methodCodeLineList = getMethodCodeLineList( startPCIndex, startPCIndexMethodCodeMap );
                methodCodeLineList.add( new MethodCodeLine( new StringBuilder( "}while( " ).append( value1 ).append( symbol ).append( value2 ).append( " );" ).append( " \n" ), false, true ) );
            }
        }
        else
        {
            if ( byteCodeInstraction.opCode.equals( "9B" ) )
            {
                //icmplt
                symbol = " >= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "9D" ) )
            {
                //icmpgt
                symbol = " <= ";
            }
            else if ( byteCodeInstraction.opCode.equals( "99" ) || byteCodeInstraction.opCode.equals( "C6" ) )
            {
                symbol = " != ";
                if ( byteCodeInstraction.opCode.equals( "C6" ) )
                {
                    value2 = "null";
                }
            }
            else if ( byteCodeInstraction.opCode.equals( "9A" ) || byteCodeInstraction.opCode.equals( "C7" ) )
            {
                symbol = " == ";
                if ( byteCodeInstraction.opCode.equals( "C7" ) )
                {
                    value2 = "null";
                }
            }
            else if ( byteCodeInstraction.opCode.equals( "9C" ) )
            {
                symbol = " < ";
            }
            else if ( byteCodeInstraction.opCode.equals( "9E" ) )
            {
                symbol = " > ";
            }
            processIfCondition( methodDetail, byteCodeInstraction, startPCIndexMethodCodeMap, startPCIndex, value1, value2, symbol );
        }
    }

    private List< MethodCodeLine > getMethodCodeLineList( int startPCIndex, Map< Integer, List< MethodCodeLine >> startPCIndexMethodCodeMap )
    {
        List< MethodCodeLine > methodCodeLineList = startPCIndexMethodCodeMap.get( startPCIndex );
        if ( methodCodeLineList == null )
        {
            methodCodeLineList = new ArrayList< MethodCodeLine >();
            startPCIndexMethodCodeMap.put( startPCIndex, methodCodeLineList );
        }
        return methodCodeLineList;

    }

    private boolean isConditionalBranching( String opCode )
    {
        return opCode.equals( "A0" ) || opCode.equals( "A1" ) || opCode.equals( "A2" ) || opCode.equals( "A3" ) || opCode.equals( "A4" ) || opCode.equals( "A5" ) || opCode.equals( "A6" ) || opCode.equals( "9F" ) || opCode.equals( "99" ) || opCode.equals( "9A" ) || opCode.equals( "9B" ) || opCode.equals( "9C" ) || opCode.equals( "9D" ) || opCode.equals( "9E" );
    }
    
    protected String getNameFromNameIndex( int nameIndex )
    {
        ConstantUtf8 data = byteCodeParser.getConstantUtf8Map().get( nameIndex );
        return data.utf8String;
    }

}
