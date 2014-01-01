package javadecompiler.bytecode.entities;


public class MethodCodeLine
{

    public StringBuilder codeLine;
    public boolean isCurlyBracketStarted;
    public boolean isCurlyBracketEnded;

    public MethodCodeLine( StringBuilder codeLine )
    {
        this.codeLine = codeLine;
    }

    public MethodCodeLine( StringBuilder codeLine, boolean isCurlyBracketStarted, boolean isCurlyBracketEnded )
    {
        this.codeLine = codeLine;
        this.isCurlyBracketEnded = isCurlyBracketEnded;
        this.isCurlyBracketStarted = isCurlyBracketStarted;
    }
}
