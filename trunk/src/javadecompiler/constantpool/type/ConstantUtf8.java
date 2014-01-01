package javadecompiler.constantpool.type;

public class ConstantUtf8
{
    public String utf8String;
    public int utfLen;

    public ConstantUtf8( int utfLen, String utf8String )
    {

        this.utfLen = utfLen;
        this.utf8String = utf8String;
    }
}