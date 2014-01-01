package javadecompiler.bytecode.parser;

public class ParseHelper
{
    public static String padWithZero( String hexaString )
    {
        hexaString = hexaString.toUpperCase();
        if ( hexaString.length() == 1 )
        {
            hexaString = "0" + hexaString;
        }
        return hexaString;
    }
}
