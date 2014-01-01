package javadecompiler.constantpool.type;

public class ConstantFieldref
{
    public int classIndex;
    public int name_and_type_index;

    public ConstantFieldref( int classIndex, int name_and_type_index )
    {

        this.classIndex = classIndex;
        this.name_and_type_index = name_and_type_index;
    }

}
