package javadecompiler.constantpool.type;

public class ConstantNameAndType
{
    public int nameIndex;
    public int descriptor_index;

    public ConstantNameAndType( int nameIndex, int descriptor_index )
    {

        this.nameIndex = nameIndex;
        this.descriptor_index = descriptor_index;
    }
}
