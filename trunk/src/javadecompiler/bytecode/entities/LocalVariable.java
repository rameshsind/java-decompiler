package javadecompiler.bytecode.entities;

public class LocalVariable
{
    public int start_pc;
    public int length;
    public int name_index;
    public int descriptor_index;
    public int index;
    public boolean isDeclared = false;
}
