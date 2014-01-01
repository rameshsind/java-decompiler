package javadecompiler.exception;

public class DecompilerException extends Exception
{
    public DecompilerException( String str )
    {
        super( str );
    }

    public DecompilerException( Throwable t )
    {
        super( t );
    }
    
}
