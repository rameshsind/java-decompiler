package javadecompiler.bytecode.entities;

public class FieldDetail
{
    public int index;
    public String accessFlag;
    public int nameIndex;
    public int descriptorIndex;
    public int fieldSignatureIndex = -1;
    public int constantValueIndex = -1;
}
