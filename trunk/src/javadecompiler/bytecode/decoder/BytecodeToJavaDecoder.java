package javadecompiler.bytecode.decoder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javadecompiler.bytecode.parser.ByteCodeParser;
import javadecompiler.constantpool.type.ConstantClass;
import javadecompiler.constantpool.type.ConstantUtf8;

public class BytecodeToJavaDecoder 
{
    ByteCodeParser byteCodeParser;
    public String[] packageAndClassName;
    private Set< String > importItemSet = new HashSet< String >();
    private Map< String, String > classNamePackageMap = new HashMap< String, String >();
    
    public BytecodeToJavaDecoder( ByteCodeParser byteCodeParser )
    {
        this.byteCodeParser = byteCodeParser;
    }

    public String getClassString()
    {
        String classStr = DecodeHelper.getAccessSpecifier( byteCodeParser.getAccessFlag(), false );
        packageAndClassName = getPackageAndClassName();
        String[] packageAndSuperClassName = getSuperClassName();
        return classStr + packageAndClassName[1] + packageAndSuperClassName[1];
    }

    private String[] getSuperClassName()
    {
        String[] name = new String[2];
        int superClassIndex = byteCodeParser.getSuperClassIndex();
        ConstantClass cc = byteCodeParser.getConstantClassMap().get( superClassIndex );
        ConstantUtf8 cUtf8 = byteCodeParser.getConstantUtf8Map().get( cc.nameIndex );

        if ( "java/lang/Object".equals( cUtf8.utf8String ) )
        {
            return new String[] { "", "" };
        }
        int index = cUtf8.utf8String.lastIndexOf( "/" );

        if ( index == -1 )
        {
            name[0] = "";
            name[1] = cUtf8.utf8String;
            return name;
        }
        name[1] = " extends " + extractClassAndPackage( cUtf8.utf8String );
        name[0] = cUtf8.utf8String.substring( 0, index ).replaceAll( "/", "." );
        return name;
    }

    public String getAllFieldDetails()
    {
        FieldJavaDecoder fieldsJavaDecoder = new FieldJavaDecoder( byteCodeParser, this );
        return fieldsJavaDecoder.decode();
    }

    public String getAllInterfaceDetails()
    {
        InterfaceJavaDecoder interfacesJavaDecoder = new InterfaceJavaDecoder( byteCodeParser, this );
        return interfacesJavaDecoder.decode();
    }

    

    private String[] getPackageAndClassName()
    {
        String[] name = new String[2];
        int thisClassIndex = byteCodeParser.getThisClassIndex();
        ConstantClass cc = byteCodeParser.getConstantClassMap().get( thisClassIndex );
        ConstantUtf8 cUtf8 = byteCodeParser.getConstantUtf8Map().get( cc.nameIndex );
        int index = cUtf8.utf8String.lastIndexOf( "/" );
        if ( index == -1 )
        {
            name[0] = "";
            name[1] = cUtf8.utf8String;
            return name;
        }
        extractClassAndPackage( cUtf8.utf8String );
        name[1] = cUtf8.utf8String.substring( index + 1 );
        name[0] = cUtf8.utf8String.substring( 0, index ).replaceAll( "/", "." );
        return name;
    }

    public String getAllMethodDetails()
    {
        MethodJavaDecoder methodsJavaDecoder = new MethodJavaDecoder(byteCodeParser, this);
        return methodsJavaDecoder.decode();
    }



    private void addImportItem( String packageName )
    {
        if ( !packageName.equals( packageAndClassName[0] ) )
            importItemSet.add( packageName );
    }

    public String getImportItems()
    {
        StringBuilder imports = new StringBuilder();
        for ( String importItem : importItemSet )
        {
            imports.append( "import " ).append( importItem ).append( ".*;\n" );
        }
        return imports.toString();
    }
    
    public String getFormatedFieldType( String returnType )
    {
        return getFormatedFieldType( returnType, true );
    }
    public String getFormatedFieldType( String fieldType, boolean isDeclaration )
    {
        String arrayStr = "";
        if ( fieldType.startsWith( "[" ) )
        {
            int i = -1;
            for ( char c : fieldType.toCharArray() )
            {
                if ( c == '[' )
                {
                    i++;
                    if ( isDeclaration )
                        arrayStr += "[]";
                }
                else
                {
                    if ( i != -1 )
                    {
                        fieldType = fieldType.substring( i + 1 );
                    }
                    break;
                }
            }
        }

        if ( "V".equals( fieldType ) )
        {
            return "void";
        }
        if ( "I".equals( fieldType ) )
        {
            return "int" + arrayStr;
        }
        else if ( "B".equals( fieldType ) )
        {
            return "byte" + arrayStr;
        }
        else if ( "Z".equals( fieldType ) )
        {
            return "boolean" + arrayStr;
        }
        else if ( "C".equals( fieldType ) )
        {
            return "char" + arrayStr;
        }
        else if ( "F".equals( fieldType ) )
        {
            return "float" + arrayStr;
        }
        else if ( "D".equals( fieldType ) )
        {
            return "double" + arrayStr;
        }
        else if ( "J".equals( fieldType ) )
        {
            return "long" + arrayStr;
        }
        else if ( "S".equals( fieldType ) )
        {
            return "short" + arrayStr;
        }
        else if ( fieldType.startsWith( "L" ) )
        {
            return extractClassAndPackage( fieldType.substring( 1, fieldType.lastIndexOf( ';' ) ) ) + arrayStr;
        }
        return fieldType;
    }
    
    String extractClassAndPackage( String classFullName )
    {
        int index = classFullName.lastIndexOf( "/" );
        if ( index == -1 )
        {
            return classFullName;
        }
        String className = classFullName.substring( index + 1 );
        String packageName = classFullName.substring( 0, index ).replaceAll( "/", "." );
        String existingPackageName = classNamePackageMap.get( className );
        if( existingPackageName == null )
        {
            addImportItem( packageName );
            classNamePackageMap.put( className, packageName );
            return className;
        }
        else if ( !existingPackageName.equals( packageName ) )
        {
            return packageName+"."+className;
        }
        else
        {
            return className;
        }
        
    }
    
   
}
