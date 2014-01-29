package javadecompiler.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javadecompiler.JavaDecompiler;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class JavaDecompilerGUI extends JFrame implements ActionListener
{
    private JTextArea taJava;
    private JTextArea taByteCode;
    private JTextArea taCurrent;
    private JMenuBar menuBar;
    private JMenu fileM, editM;
    private JScrollPane scpane;
    private JScrollPane scpaneByteCode;
    private JMenuItem exitI, cutI, copyI, pasteI, selectI, saveI, openI;
    private String pad;
    private JToolBar toolBar;
    final JFileChooser fc;
    private String inputFileName = null;
    Container pane = null;
    String currentJavaText = "";
    String currentByteCodeText = "";

    public JavaDecompilerGUI()
    {
        super( "Java Decomplier" );
        setSize( 600, 600 );
        setLocationRelativeTo( null );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        pane = getContentPane();
        pane.setLayout( new BorderLayout() );

        fc = new JFileChooser();
        pad = " ";
        taCurrent = taJava = new JTextArea(); //textarea
        taByteCode = new JTextArea();

        menuBar = new JMenuBar(); //menubar
        fileM = new JMenu( "File" ); //file menu
        editM = new JMenu( "Edit" ); //edit menu
        scpane = new JScrollPane( taJava ); //scrollpane  and add textarea to scrollpane
        scpaneByteCode = new JScrollPane( taByteCode );

        exitI = new JMenuItem( "Exit" );
        cutI = new JMenuItem( "Cut" );
        copyI = new JMenuItem( "Copy" );
        pasteI = new JMenuItem( "Paste" );
        selectI = new JMenuItem( "Select All" ); //menuitems
        saveI = new JMenuItem( "Save As" ); //menuitems
        openI = new JMenuItem( "Open" ); //menuitems
        toolBar = new JToolBar();

        setJMenuBar( menuBar );
        menuBar.add( fileM );
        menuBar.add( editM );

        fileM.add( openI );
        fileM.add( saveI );
        fileM.add( exitI );

        editM.add( cutI );
        editM.add( copyI );
        editM.add( pasteI );
        editM.add( selectI );

        saveI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
        openI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_L, ActionEvent.CTRL_MASK ) );
        cutI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, ActionEvent.CTRL_MASK ) );
        copyI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, ActionEvent.CTRL_MASK ) );
        pasteI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_V, ActionEvent.CTRL_MASK ) );
        selectI.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_A, ActionEvent.CTRL_MASK ) );

        //Create top node of a tree
        final DefaultMutableTreeNode top = new DefaultMutableTreeNode( "Decoded" );

        //Create a subtree Java
        final DefaultMutableTreeNode javaNode = new DefaultMutableTreeNode( "Java" );
        top.add( javaNode );
        final DefaultMutableTreeNode byteCodeNode = new DefaultMutableTreeNode( "ByteCode" );
        top.add( byteCodeNode );

        //Creating tree
        final JTree tree = new JTree( top );

        int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        final JScrollPane jsp = new JScrollPane( tree, v, h );
        tree.addMouseListener( new MouseAdapter()
        {
            public void mouseClicked( MouseEvent me )
            {
                TreePath tp = tree.getPathForLocation( me.getX(), me.getY() );
                if ( tp == null )
                    return;
                if ( tp.toString().equals( "[Decoded, Java]" ) )
                {
                    pane.remove( scpaneByteCode );
                    pane.add( scpane, BorderLayout.CENTER );
                    pane.validate();
                    pane.repaint();
                    taCurrent = taJava;
                }
                else if ( tp.toString().equals( "[Decoded, ByteCode]" ) )
                {
                    pane.remove( scpane );
                    pane.add( scpaneByteCode, BorderLayout.CENTER );
                    pane.validate();
                    pane.repaint();
                    taCurrent = taByteCode;
                }
            }
        } );

        pane.add( scpane, BorderLayout.CENTER );
        pane.add( toolBar, BorderLayout.SOUTH );
        pane.add( jsp, BorderLayout.WEST );

        saveI.addActionListener( this );
        openI.addActionListener( this );
        exitI.addActionListener( this );
        cutI.addActionListener( this );
        copyI.addActionListener( this );
        pasteI.addActionListener( this );
        selectI.addActionListener( this );

        setVisible( true );
    }

    public void actionPerformed( ActionEvent e )
    {
        JMenuItem choice = (JMenuItem)e.getSource();
        if ( choice == openI )
        {
            int returnVal = fc.showOpenDialog( this );

            if ( returnVal == JFileChooser.APPROVE_OPTION )
            {
                File file = fc.getSelectedFile();
                inputFileName = file.getAbsolutePath();
                JavaDecompiler jd = new JavaDecompiler();
                taJava.setText( null );
                taByteCode.setText( null );
                StringBuilder[] output = { new StringBuilder(), new StringBuilder() };
                try
                {
                    jd.decompile( inputFileName, output );
                }
                catch ( Throwable t )
                {
                    JOptionPane.showMessageDialog( this, "Unable to decode the input class file" );
                }
                taJava.append( output[1].toString() );
                taJava.setCaretPosition( 0 );
                taByteCode.append( output[0].toString() );
                taByteCode.setCaretPosition( 0 );
            }
            else
            {
                //ta.append( "Open command cancelled by user." );
            }
        }
        else if ( choice == saveI )
        {
            if ( inputFileName == null )
                return;
            File outFile = new File( inputFileName + ( ( taCurrent == taJava ) ? ".java" : ".bytecode.txt" ) );
            fc.setSelectedFile( outFile );
            if ( fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
            {

                FileWriter fw = null;
                try
                {
                    fw = new FileWriter( fc.getSelectedFile() );
                    fw.write( taCurrent.getText() );

                }
                catch ( IOException e1 )
                {
                    JOptionPane.showMessageDialog( this, "Unable to save" );
                    e1.printStackTrace();
                }
                finally
                {
                    if ( fw != null )
                        try
                        {
                            fw.close();
                        }
                        catch ( IOException e1 )
                        {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                }
            }
        }
        else if ( choice == exitI )
        {
            System.exit( 0 );
        }
        else if ( choice == cutI )
        {
            pad = taCurrent.getSelectedText();
            taCurrent.replaceRange( "", taCurrent.getSelectionStart(), taCurrent.getSelectionEnd() );
        }
        else if ( choice == copyI )
            pad = taCurrent.getSelectedText();
        else if ( choice == pasteI )
            taCurrent.insert( pad, taCurrent.getCaretPosition() );
        else if ( choice == selectI )
            taCurrent.selectAll();

    }

    public static void main( String[] args )
    {
        new JavaDecompilerGUI();
    }

}