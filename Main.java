import syntaxtree.*;
import java.io.*;
import java.lang.Object.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main [file1] [file2] ... [fileN] ");
            System.exit(1);
        }

        // tables and files
        FileInputStream fis = null;
        Taburu symboru_taburu = null;
        V_Taburu virtual_taburu = null;
        FileWriter patrick_rothfuss = null;

        // for each file
        for (String da_file : args) {

            try {
                fis = new FileInputStream(da_file);
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                symboru_taburu = new Taburu();
                virtual_taburu = new V_Taburu();

                // fill the symbol table
                Collector eval = new Collector(symboru_taburu);
                root.accept(eval, null);

                // do the type checking
                Typechecker another_round = new Typechecker(symboru_taburu);
                root.accept(another_round, null);

                // if no exception till this point then no problem
                System.err.println("Java file: " + da_file + " is semantically correct.");

                // comment out to not print the offsets
                symboru_taburu.print_class_offsets();
                System.out.println();

                // fill the virtual table
                Gatherer extra_round = new Gatherer(symboru_taburu, virtual_taburu);
                root.accept(extra_round, null);

                // remove java extension add ll
                String book = da_file.substring(0, da_file.lastIndexOf('.'))+".ll";

                // translation to LLVM and write to the file
                patrick_rothfuss = new FileWriter(book);
                Translator final_round = new Translator(symboru_taburu, virtual_taburu, patrick_rothfuss);
                root.accept(final_round, null);

                // if no exception till this point then the code was generated
                System.err.println("Conversion complete! LLVM code generated in: "+book);

            }
            catch (Exception ex) {

                System.err.println("Error: " + ex.getMessage());
                System.err.println();
            
            }
            finally {

                try {
                
                    if (fis != null) fis.close();
                    if (patrick_rothfuss != null) patrick_rothfuss.close();
                
                } catch (IOException ex) {
                
                    System.err.println(ex.getMessage());
                
                }
            
            }
        
        }
    
    }

}

