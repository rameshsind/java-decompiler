package javadecompiler.bytecode.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MethodDetail
{
    public int index;
    public String accessFlag;
    public int nameIndex;
    public int descriptorIndex;

    public CodeLineNumber[] codeLineNumbers;
    public Map< Integer, CodeLineNumber > startPcCodeLineNumberMap;

    public List< ByteCodeInstraction > byteCodeInstractionList;
    public Map< Integer, ByteCodeInstraction > startPcByteCodeInstractionMap;

    public Map< Integer, List< LocalVariable > > indexLocalVariableMap = new HashMap< Integer, List< LocalVariable > >();
   // public LocalVariable[] localVariables;
    public int[] exceptions = null;
    public Set<Integer> tryStartPcSet = new HashSet< Integer >();
    public Set<Integer> tryEndStartPcSet = new HashSet< Integer >();
    public Set<Integer> expHandlePcSet = new TreeSet< Integer >();
    
}
