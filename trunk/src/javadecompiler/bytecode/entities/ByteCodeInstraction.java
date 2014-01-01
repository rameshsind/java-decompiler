package javadecompiler.bytecode.entities;

public class ByteCodeInstraction
{
    public int startPCIndex;
    public String opCode;
    public int length;
    public String operands[];
    public short branchOffset = 0;
    public int previousInstructionStartPCIndex;
    public boolean isLoop = false;
      

}
