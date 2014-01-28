package javadecompiler.bytecode.decoder;


public class DecodeHelper
{
    public static final String FORMATTED_SPACE = "          ";
    
    public static String getAccessSpecifier( String accessHexaCode, boolean isMethod )
    {
        StringBuilder result = new StringBuilder();
        result.append( getAccessSpecifierForDigit4( accessHexaCode.substring( 3 ) ) );
        result.append( getAccessSpecifierForDigit2( accessHexaCode.substring( 1, 2 ) ) );
        result.append( getAccessSpecifierForDigit3( accessHexaCode.substring( 2, 3 ), isMethod ) );
        return result.toString();
    }

    public static String getFieldAccessSpecifier( String accessHexaCode )
    {
        StringBuilder result = new StringBuilder();
        result.append( getAccessSpecifierForDigit4( accessHexaCode.substring( 3 ) ) );
        result.append( getAccessSpecifierForDigit3( accessHexaCode.substring( 2, 3 ), false ) );
        return result.toString();
    }

    private static String getAccessSpecifierForDigit4( String digit4 )
    {
        if ( "1".equals( digit4 ) )
        {
            return "public ";
        }
        else if ( "2".equals( digit4 ) )
        {
            return "private ";
        }
        else if ( "4".equals( digit4 ) )
        {
            return "protected ";
        }
        else if ( "8".equals( digit4 ) )
        {
            return "static ";
        }
        else if ( "9".equals( digit4 ) )
        {
            return "public static ";
        }
        else if ( "A".equals( digit4 ) )
        {
            return "private static ";
        }
        else if ( "C".equals( digit4 ) )
        {
            return "protected static ";
        }
        return "";
    }

    private static String getAccessSpecifierForDigit2( String digit )
    {
        if ( "1".equals( digit ) )
        {
            return "native ";
        }
        else if ( "2".equals( digit ) )
        {
            return "interface ";
        }
        else if ( "4".equals( digit ) )
        {
            return "abstract ";
        }
        else if ( "5".equals( digit ) )
        {
            return "abstract native ";
        }
        else if ( "6".equals( digit ) )
        {
            return "abstract interface ";
        }
    
        return "";
    }

    private static String getAccessSpecifierForDigit3( String digit, boolean isMethod )
    {
        if ( "1".equals( digit ) )
        {
            return "final ";
        }
        else if ( "2".equals( digit ) )
        {
            if ( isMethod )
            {
                return "synchronized ";
            }
            else
            {
                return "class ";
            }
    
        }
        else if ( "3".equals( digit ) )
        {
            if ( isMethod )
            {
                return "final synchronized ";
            }
            else
            {
                return "final class ";
            }
    
        }
        else if ( "4".equals( digit ) )
        {
            return "volatile ";
        }
        else if ( "8".equals( digit ) )
        {
            return "transient ";
        }
        else if ( "9".equals( digit ) )
        {
            return "final transient ";
        }
        else if ( "C".equals( digit ) )
        {
            return "volatile transient ";
        }
        else if ( "D".equals( digit ) )
        {
            return "final volatile transient ";
        }
    
        return "";
    }

    protected static String getFormatedSpace( int bracketCount )
    {
        StringBuilder formateSpace = new StringBuilder();
        for ( int count = 0; count < bracketCount; count++ )
        {
            formateSpace.append( FORMATTED_SPACE );
        }
        return formateSpace.toString();
    }

    public static String getAType( int atype )
    {
        switch ( atype )
        {
            case 4:
                return "boolean";
            case 5:
                return "char";
            case 6:
                return "float";
            case 7:
                return "double";
            case 8:
                return "byte";
            case 9:
                return "short";
            case 10:
                return "int";
            case 11:
                return "long";
        }
        return "";
    }
}