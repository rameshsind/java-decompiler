package javadecompiler.bytecode.parser.opcode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import javadecompiler.bytecode.parser.ParseHelper;
import javadecompiler.exception.DecompilerException;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class JavaOpcodeLoader
{
    
    public static void loadJVMInsruction(Map< String, Opcode > opcodeInfoMap) throws DecompilerException
    {
        InputStream inputFile = JavaOpcodeLoader.class.getResourceAsStream("/javadecompiler/bytecode/parser/opcode/jvminstruction.xls");
        WorkbookSettings ws = new WorkbookSettings();
        ws.setLocale( new Locale( "en", "EN" ) );

        Workbook workbook = null;
        try
        {
            workbook = Workbook.getWorkbook( inputFile, ws );
        }
        catch ( BiffException e )
        {
            e.printStackTrace();
            throw new DecompilerException(e);
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new DecompilerException(e);
        }

        Sheet s1 = workbook.getSheet( 0 );
        loadBycode( s1, opcodeInfoMap );

    }

    public static void loadBycode( Sheet s, Map< String, Opcode > opcodeInfoMap )
    {
        int columns = s.getColumns();
        int rows = s.getRows();
        for ( int row = 1; row < rows; row++ )
        {
            String hexaCode = "";
            String opCode = "";
            int operandCount = 0;
            String description = "";

            for ( int column = 0; column < columns; column++ )
            {
                Cell c = s.getCell( column, row );
                String content = c.getContents();
                if ( column == 0 && content != null && !content.equals( "" ) )
                {
                    opCode = content;
                }
                else if ( column == 1 && content != null && !content.equals( "" ) )
                {
                    hexaCode = ParseHelper.padWithZero( content );
                }
                else if ( column == 3 && content != null && !content.equals( "" ) )
                {
                    operandCount = Integer.parseInt( content.trim() );
                }
                else if ( column == 5 && content != null && !content.equals( "" ) )
                {
                    description = content;
                }
            }

            opcodeInfoMap.put( hexaCode, new Opcode( opCode, operandCount, description ) );

        }
    }

   

}
