package javadecompiler.bytecode.parser.opcode;

public class Opcode
{
    public String opcode;
    public int operandCount;
    public String description;

    public Opcode( String opcode, int operandCount, String description )
    {

        this.opcode = opcode;
        this.operandCount = operandCount;
        this.description = description;
    }

}
