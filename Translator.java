import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.*;
import java.io.*;

public class Translator extends GJDepthFirst<String, String> {

    // symbol and virtual table
    private Taburu taburu;
    private V_Taburu v_taburu;
    private FileWriter rothfuss;

    public Translator(Taburu symboru, V_Taburu v_taburu1, FileWriter patrick){
        this.taburu = symboru;
        this.v_taburu = v_taburu1;
        this.rothfuss = patrick;
    }

    // emit the LLVM type from the minijava type
    private void emitType(String MJtype) throws Exception{
        rothfuss.write(MJtoLL(MJtype));
    }

    // write to the file
    private void emit (String write_this) throws Exception{
        rothfuss.write(write_this);
    }

    // Because registers are single-assignment, you will probably need to
    // keep a counter to produce new ones.
    private int register_counter = 0;

    // t stands for temporary registers (like riscv)
    private String register(){
        String typeR = "%_t"+register_counter;
        register_counter+=1;
        return typeR;
    }

    // minijava to LLVM type
    private String MJtoLL(String MJtype){

        // int, boolean, the struct arrays and pointer
        if(MJtype.equals("int")) {
            return "i32";
        }
        else if(MJtype.equals("boolean")) {
            return "i1";
        }
        else if(MJtype.equals("int[]")) {
            return "%_IntegerArray*";
        }
        else if(MJtype.equals("boolean[]")) {
            return "%_BooleanArray*";
        }
        return "i8*";
    
    }

    // control flow label counters
    private int label_counter = 0;

    // beginning with LBL and then its number
    private String label(){
        String labelito = "LBL"+label_counter;
        label_counter+=1;
        return labelito; 
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        String class_name = n.f1.accept(this, "business_as_usual");

        // struct array declarations first
        emit("%_IntegerArray = type { i32, [0 x i32] }\n");
        emit("%_BooleanArray = type { i32, [0 x i1]  }\n\n");

        // then the classes virtual tables
        for (String classNameVT: v_taburu.get_all_classes()){

            emit("@."+classNameVT+"_vtable = global ["+ v_taburu.get_virtual_table(classNameVT).size() + " x i8*] \n[");

            int count =0;

            // for every method of this class
            for (Map.Entry<String,String> entry : v_taburu.get_virtual_table(classNameVT).entrySet()) {

                // to separate with comma
                if (count!=0){
                    emit(",\n ");
                }
                count+=1;

                // get the method type from the symbol table
                emit("i8* bitcast (");
                emitType(taburu.get_class(entry.getValue()).get_method_type(entry.getKey()));
                emit(" (i8*");

                // get the argument type from the symbol table
                for(int i=0; i< taburu.get_class(entry.getValue()).get_arg_size(entry.getKey()); i++){
                    emit(", ");
                    emitType(taburu.get_class(entry.getValue()).get_arg_type(entry.getKey(), i));
                }

                // class name that method inherits from
                emit(")* @"+ entry.getValue()+"."+entry.getKey()+" to i8*)");

            }

            emit("]\n\n");

        }

        // external method declarations
        emit("declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
        emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n\ndefine void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n");

        // define main
        emit("define i32 @main() {\n");

        // to emit them recursively
        n.f14.accept(this,  "main:imamethodof:"+class_name);
        n.f15.accept(this,  "main:imamethodof:"+class_name);

        emit("\n\tret i32 0 \n} \n");

        return null;
    }



    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String class_name = n.f1.accept(this, "business_as_usual");

        // class name is needed for method declaration
        n.f4.accept(this, class_name);

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String class_name = n.f1.accept(this, null);

        // class name is needed for method declaration
        n.f6.accept(this, class_name);

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String my_class = argu;
        String my_type = n.f1.accept(this, "business_as_usual");
        String my_name = n.f2.accept(this, "business_as_usual");

        // class name, method name and this in the LLVM standards
        emit("\ndefine "+my_type+" @"+ my_class+"."+my_name+"(i8* %this");

        // then the arguments
        for(int i=0; i< taburu.get_class(my_class).get_arg_size(my_name); i++){
            emit(", ");
            emitType(taburu.get_class(my_class).get_arg_type(my_name, i));
            emit(" %."+taburu.get_class(my_class).get_arg_name(my_name, i));
        }
        emit("){\n\n");

        // alloca and store for the arguments
        for(int i=0; i< taburu.get_class(my_class).get_arg_size(my_name); i++){
            emit("\t%"+taburu.get_class(my_class).get_arg_name(my_name, i));
            emit(" = alloca ");
            emitType(taburu.get_class(my_class).get_arg_type(my_name, i));
            emit("\n\tstore ");
            emitType(taburu.get_class(my_class).get_arg_type(my_name, i));
            emit(" %."+taburu.get_class(my_class).get_arg_name(my_name, i)+", ");
            emitType(taburu.get_class(my_class).get_arg_type(my_name, i));
            emit("* %"+taburu.get_class(my_class).get_arg_name(my_name, i)+"\n");
        }
        
        // var declarations
        n.f7.accept(this, my_name + ":imamethodof:"+ argu);

        // statements 
        n.f8.accept(this, my_name + ":imamethodof:"+ argu);
        
        // expression to return
        String hold_this = n.f10.accept(this, my_name + ":imamethodof:"+ argu);
        
        // string manipulation to get the necessary information
        String ret_type = hold_this.split(":")[2];
        String ret_reg = hold_this.split(":")[0]; 

        emit("\n\tret "+ret_type+ " "+ret_reg+" \n}\n");

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        String my_type = n.f0.accept(this, "business_as_usual");
        String my_name = n.f1.accept(this, "business_as_usual");

        emit("\t%"+my_name+" = alloca "+my_type+"\n\tstore "+my_type+" ");

        // Everything new in Java is initialized to zeroes.
        if (my_type.equals("i1")||my_type.equals("i32")){
            emit("0");
        }
        else {
            emit("null");
        }

        if(!my_name.split(":")[0].contains("%"))
            emit(", "+my_type+"* %"+my_name+"\n\n");
        else
            emit(", "+my_type+"* "+my_name+"\n\n");

        return null;
    }


   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String argu) throws Exception {

        String name = n.f0.accept(this, "business_as_usual");
        
        // expression
        String expr = n.f2.accept(this, argu);

        // to get the register expression
        String exp_reg = expr.split(":")[0];
        String exp_type = expr.split(":")[2];

        // try because these functions might throw an exception
        try {
            String var_type = taburu.find_me(name, argu);
            
            String llt = MJtoLL(var_type);

            int my_offset = taburu.find_my_offset(name, argu);

            if(my_offset != -1 ){

                String r1 = register();
                String r2 = register();

                // getelementptr is used to get the pointer to an element of an
                // array from a pointer to that array and the index of the element.
                // Example: %ptr_idx = getelementptr i8, i8* %ptr, i32 %idx
                emit("\t"+r1+" = getelementptr i8, i8* %this, i32 "+ my_offset+"\n");

                // bitcast is used to cast between different pointer types. It takes
                // the value and type to be cast, and the type that it will be cast to.
                // Example: %ptr = bitcast i32* %ptr2 to i8**
                emit("\t"+r2+" = bitcast i8* "+r1+" to "+llt+"*\n");

                emit("\tstore "+exp_type+" "+exp_reg+", "+ llt+"* "+r2+"\n\n");

                // format for consistency "register:whereitcamefrom:i32:int"
                return r2+":ASGNMNT:"+llt+":"+var_type;
                
            }

            if(!name.contains("%"))
                emit("\tstore "+exp_type+" "+exp_reg+", "+ llt+"* %"+name+"\n\n");
            else
                emit("\tstore "+exp_type+" "+exp_reg+", "+ llt+"* "+name+"\n\n");

            // format for consistency "register:whereitcamefrom:i32:int"
            return name+":ASGNMNT:"+llt+":"+var_type;
        
        } catch (Exception e) {
            return "Error in assignment.";
        }

     }
  
     /**
      * f0 -> Identifier()
      * f1 -> "["
      * f2 -> Expression()
      * f3 -> "]"
      * f4 -> "="
      * f5 -> Expression()
      * f6 -> ";"
      */
     public String visit(ArrayAssignmentStatement n, String argu) throws Exception {

        // to get the indentifier name
        String array_ident = n.f0.accept(this, argu);
        String array_index = n.f2.accept(this, argu);
        String assgnmt_expr = n.f5.accept(this, argu);

        // two labels for negative check
        String l1 = label();
        String l2 = label();

        // two labels for within bounds check
        String l3 = label();
        String l4 = label();

        // registers
        String t1 = register();
        String t5 = register();
        String t6 = register();
        String t7 = register();
        String t8 = register();

        // identifier register
        String id_reg = array_ident.split(":")[0]; 

        // index register or the literal itself
        String ar_indx_reg = array_index.split(":")[0];

        // assignment register
        String assgnmt_reg = assgnmt_expr.split(":")[0];

        if(array_ident.split(":")[3].equals("boolean[]")){

            // negative check >=0
            emit("\t"+t1+" = icmp sge i32 "+ar_indx_reg+", 0\n");
            emit("\tbr i1 "+t1+", label %"+l2+ ", label %"+l1 +"\n\n");

            // l1 that its negative 
            emit(l1+":\n");
            emit("\tcall void @throw_oob()\n\tbr label %"+l2+"\n\n");

            // label second its not
            emit(l2+":\n");

            // to get the first struct element
            emit("\t"+t5+" = getelementptr %_BooleanArray, %_BooleanArray* "+id_reg+", i32 0, i32 0 \n");

            // t6 the size of Identifier array
            emit("\t"+t6+" = load i32, i32* "+t5+"\n\n");

            // check index < array size
            emit("\t"+t7+" = icmp slt i32 "+ar_indx_reg+", "+ t6+"\n");
            emit("\tbr i1 "+t7+", label %"+l4+ ", label %"+l3 +"\n\n");

            // label l3 out of bounds 
            emit(l3+":\n");
            emit("\tcall void @throw_oob()\n\tbr label %"+l4+"\n\n");

            // label l4 in bounds
            emit(l4+":\n");
            
            // store it
            emit("\t"+t8+ " = getelementptr %_BooleanArray, %_BooleanArray* "+id_reg+", i32 0, i32 1, i32 "+ar_indx_reg+"\n\n");
            emit("\t store i1 "+assgnmt_reg+", i1* "+t8+"\n\n");

        }
        // for integer array
        else{

            // negative check >=0
            emit("\t"+t1+" = icmp sge i32 "+ar_indx_reg+", 0\n");
            emit("\tbr i1 "+t1+", label %"+l2+ ", label %"+l1 +"\n\n");

            // label l1 negative
            emit(l1+":\n");
            emit("\tcall void @throw_oob()\n\tbr label %"+l2+"\n\n");

            // label l2 its not
            emit(l2+":\n");

            // first struct element
            emit("\t"+t5+" = getelementptr %_IntegerArray, %_IntegerArray* "+id_reg+", i32 0, i32 0 \n");

            // t6 the size of Identifier array
            emit("\t"+t6+" = load i32, i32* "+t5+"\n\n");

            // check index < array size
            emit("\t"+t7+" = icmp slt i32 "+ar_indx_reg+", "+ t6+"\n");
            emit("\tbr i1 "+t7+", label %"+l4+ ", label %"+l3 +"\n\n");

            // label l3 out of bounds 
            emit(l3+":\n");
            emit("\tcall void @throw_oob()\n\tbr label %"+l4+"\n\n");

            // label l4 in bounds
            emit(l4+":\n");

            // store it
            emit("\t"+t8+ " = getelementptr %_IntegerArray, %_IntegerArray* "+id_reg+", i32 0, i32 1, i32 "+ar_indx_reg+"\n\n");
            emit("\t store i32 "+assgnmt_reg+", i32* "+t8+"\n\n");

        }

        return null;

     }
  
     /**[]
      * f0 -> "if"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> Statement()
      * f5 -> "else"
      * f6 -> Statement()
      */
     public String visit(IfStatement n, String argu) throws Exception {
        String expr= n.f2.accept(this, argu);

        // no need to check that its boolean (done it in the typechecker visitor)
        String eReg = expr.split(":")[0];

        String l1_exit = label();
        String l2_true = label();
        String l3_false = label();

        emit("\tbr i1 "+eReg+", label %"+l2_true+", label %"+l3_false+"\n\n");

        // l2 label
        emit(l2_true+":\n");

        n.f4.accept(this, argu);

        emit("\tbr label %"+l1_exit+"\n");

        // l3 label
        emit(l3_false+":\n");

        // statement
        n.f6.accept(this, argu);

        emit("\tbr label %"+l1_exit+"\n");

        // l1 label
        emit(l1_exit+":\n");
        
        return null;
     }
  
     /**
      * f0 -> "while"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> Statement()
      */
     public String visit(WhileStatement n, String argu) throws Exception {

        String l1_while = label();
        String l2_true = label();
        String l3_false = label();

        emit("\tbr label %"+l1_while+"\n");

        // l1 label
        emit(l1_while+":\n");

        // no need to check that its boolean (done it in the typechecker visitor)
        String expr= n.f2.accept(this, argu);

        // get the register
        String eReg = expr.split(":")[0];

        // WHILE TRUE l2 else l3
        emit("\tbr i1 " + eReg+", label %"+l2_true +", label %"+ l3_false+"\n\n");

        // l2 label
        emit(l2_true+":\n");

        // statement
        n.f4.accept(this, argu);

        // continue to condition
        emit("\tbr label %"+l1_while+"\n");

        // l3 label
        emit(l3_false+":\n");
        
        return null;
     }
  
     /**
      * f0 -> "System.out.println"
      * f1 -> "("
      * f2 -> Expression()
      * f3 -> ")"
      * f4 -> ";"
      */
     public String visit(PrintStatement n, String argu) throws Exception {
        String _ret = n.f2.accept(this, argu);
        String register_part = _ret.split(":")[0];

        // since print only allows integers
        emit("\tcall void (i32) @print_int(i32 " + register_part + ")\n\n");
        
        return null;
     }
  
  
     /**
      * f0 -> Expression()
      * f1 -> ExpressionTail()
      */
     public String visit(ExpressionList n, String argu) throws Exception {
        String expr = n.f0.accept(this, argu);
        if (n.f1!=null) expr+= n.f1.accept(this, argu);
        return expr;
     }
  
     /**
      * f0 -> ( ExpressionTerm() )*
      */
     public String visit(ExpressionTail n, String argu) throws Exception {
        String expr = "";

        // similar to the typechecker
        for (Node nodo : n.f0.nodes)
           expr += "," +nodo.accept(this, argu);
        return expr;
     }
  
     /**
      * f0 -> ","
      * f1 -> Expression()
      */
     public String visit(ExpressionTerm n, String argu) throws Exception {
        return  n.f1.accept(this, argu);
     }


    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, String argu) throws Exception {
       
        // get the class name
        String[] bettersafethansorry = argu.split(":imamethodof:");
       
        return "%this:THIS:i8*:"+bettersafethansorry[1];
    }
  
     /**
      * f0 -> "new"
      * f1 -> "boolean"
      * f2 -> "["
      * f3 -> Expression()
      * f4 -> "]"
      */
     public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
        String expr = n.f3.accept(this, argu);
        String t1 = register();
        String t2 = register();
        String t3 = register();
        String t4 = register();
        String t5 = register();
        String l1 = label();
        String l2 = label();

        // get the size first
        String arraysize = expr.split(":")[0];

        // negative check
        emit("\t"+t1+" = icmp sge i32 "+arraysize+", 0\n");
        emit("\tbr i1 "+t1+", label %"+l2+ ", label %"+l1 +"\n\n");

        // label l1 negative
        emit(l1+":\n");
        emit("\tcall void @throw_oob()\n\tbr label %"+l2+"\n\n");

        // label l2 all good
        emit(l2+":\n");

        // +4 array size
        emit("\t"+t4+" = add i32 4, " +arraysize+"\n");
        
        emit("\t"+t2 +" = call i8* @calloc(i32 "+t4+", i32 1)\n");
        emit("\t"+t3 +" = bitcast i8* "+t2+" to %_BooleanArray*\n");

        // first index of array
        emit("\t"+t5+" = getelementptr %_BooleanArray, %_BooleanArray* "+t3+", i32 0, i32 0\n");

        emit("\tstore i32 "+arraysize+", i32* "+ t5 +"\n\n");
        return t3+":ALOC:%_BooleanArray*:boolean[]";
     }
  
     /**
      * f0 -> "new"
      * f1 -> "int"
      * f2 -> "["
      * f3 -> Expression()
      * f4 -> "]"
      */
     public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String expr = n.f3.accept(this, argu);
        String t1 = register();
        String t2 = register();
        String t3 = register();
        String t4 = register();
        String t5 = register();
        String l1 = label();
        String l2 = label();

        // get the size
        String arraysize = expr.split(":")[0];

        // negative check
        emit("\t"+t1+" = icmp sge i32 "+arraysize+", 0\n");
        emit("\tbr i1 "+t1+", label %"+l2+ ", label %"+l1 +"\n\n");

        // label l1 negative
        emit(l1+":\n");
        emit("\tcall void @throw_oob()\n\tbr label %"+l2+"\n\n");

        // label l2 its not
        emit(l2+":\n");

        // to +1 sto array size
        emit("\t"+t4+" = add i32 1, " +arraysize+"\n");
        emit("\t"+t2 +" = call i8* @calloc(i32 "+t4+", i32 4)\n");
        emit("\t"+t3 +" = bitcast i8* "+t2+" to %_IntegerArray*\n");

        // first array index
        emit("\t"+t5+" = getelementptr %_IntegerArray, %_IntegerArray* "+t3+", i32 0, i32 0\n");

        emit("\tstore i32 "+arraysize+", i32* "+ t5 +"\n\n");

        return t3+":ALOC:%_IntegerArray*:int[]";
     }
  
     /**
      * f0 -> "new"
      * f1 -> Identifier()
      * f2 -> "("
      * f3 -> ")"
      */
     public String visit(AllocationExpression n, String argu) throws Exception {

        String class_name = n.f1.accept(this, "business_as_usual");

        // to get size add 8 on the last offset
        int class_size = taburu.get_class(class_name).getVar_offset() + 8;

        // method count from the virtual table
        int method_size = v_taburu.get_method_count(class_name);

        String t1 = register();
        String t2 = register();
        String t3 = register();
        
        emit("\t"+t1+" = call i8* @calloc(i32 1, i32 " + class_size+ ")\n");
        emit("\t"+t2+" = bitcast i8* "+t1+ " to i8***\n");
        emit("\t"+t3+" = getelementptr ["+method_size +" x i8*]"+", ["+ method_size +" x i8*]* @."+class_name+"_vtable, i32 0, i32 0\n");
        emit("\tstore i8** "+t3+ ", i8*** "+t2+"\n\n");
    
        return t1+":ALOC:i8*:"+class_name;
     }
  
     /**
      * f0 -> "!"
      * f1 -> Clause()
      */
     public String visit(NotExpression n, String argu) throws Exception {
        String _ret = n.f1.accept(this, argu);
        String t1 = register();
        String id = _ret.split(":")[0];

        emit("\t"+t1+" = xor i1 1, "+id+"\n\n");

        return t1+":CLAUSE:i1:boolean";
     }
  
     /**
      * f0 -> "("
      * f1 -> Expression()
      * f2 -> ")"
      */
     public String visit(BracketExpression n, String argu) throws Exception {
        String _ret = n.f1.accept(this, argu);
        return _ret;
     }


    // Identifier returns string or emits accordingly
    @Override
    public String visit(Identifier n, String argu) {
        String ident = n.f0.toString();

        // case to return string
        if (argu==null||argu.equals("business_as_usual"))
            return ident;

        // else it's a variable that I have to emit code
        try {
            String var_type = taburu.find_me(ident, argu);
            String llt = MJtoLL(var_type);
            int my_offset = taburu.find_my_offset(ident, argu);

            if(my_offset != -1 ){

                String r1 = register();
                String r2 = register();
                String r3 = register();

                // getelementptr is used to get the pointer to an element of an
                // array from a pointer to that array and the index of the element.
                // Example: %ptr_idx = getelementptr i8, i8* %ptr, i32 %idx
                emit("\t"+r1+" = getelementptr i8, i8* %this, i32 "+ my_offset+"\n");

                // bitcast is used to cast between different pointer types. It takes
                // the value and type to be cast, and the type that it will be cast to.
                // Example: %ptr = bitcast i32* %ptr2 to i8**
                emit("\t"+r2+" = bitcast i8* "+r1+" to "+llt+"*\n");

                // load is used to load a value from a memory location. The
                // parameters are the type of the value and a pointer to the memory.
                // Example: %val = load i32, i32* %ptr
                emit("\t"+r3+" = load "+llt +", " + llt+"* "+r2+"\n\n");

                // format "register:whereicamefrom:int:i32:"
                return r3+":ID:"+llt+":"+var_type;
                
            }
            
            // if not make a new register
            String r1 = register();

            // Example: %val = load i32, i32* %ptr
            emit("\t"+r1+" = load "+llt +", " + llt+"* %"+ident+"\n\n");

            // format "register:whereicamefrom:int:i32:"
            return r1+":ID:"+llt+":"+var_type;
        
        
        } catch (Exception e) {
            return "Error in Identifier.";
        }

    }


     /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, String argu) throws Exception {
        if (n.f0.which==3) return "i8*";

        // else return one of the other types
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    public String visit(ArrayType n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    public String visit(BooleanArrayType n, String argu) {
        return "%_BooleanArray*";
    }

    public String visit(IntegerArrayType n, String argu) {
        return "%_IntegerArray*";
    }

    public String visit(BooleanType n, String argu) {
        return "i1";
    }

    public String visit(IntegerType n, String argu) {
        return "i32";
    }

    /**
    * f0 -> "INTEGER_LITERAL"
    */
   public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.tokenImage+":LITERAL:i32:int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1:LITERAL:i1:boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "0:LITERAL:i1:boolean";
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String argu) throws Exception {

        // Here we instruct the phi instruction to choose %a if the previous 
        // basic block was %btrue. If the previous basic block was %bfalse, 
        // then %b will be used. The value is then assigned to a new variable %retval.
        // %retval = phi i32 [%a, %btrue], [%b, %bfalse]

        // first do the first clause
        String clause_L = n.f0.accept(this, argu);
        String reg1 = clause_L.split(":")[0];

        // three labels for it
        String l1 = label();
        String l2 = label();
        String l3 = label();

        // reg1: the i1 of the first clause
        emit("\tbr i1 "+reg1+", label %"+l2+", label %"+l1+"\n");

        // LABEL 1
        emit(l1+":\n");

        // if false go to phi
        emit("\tbr label %"+l3+"\n");

        // LABEL 2
        emit(l2+":\n");
        
        // do the second clause
        String clause_R = n.f2.accept(this, argu);
        String reg2 = clause_R.split(":")[0];
        
        // go to phi
        emit("\tbr label %"+l3+"\n");
        
        // LABEL 3
        emit(l3+":\n");
        
        String t1 = register();

        // if Im here from label 1 false, if from label 2 just return reg2 value
        emit("\t"+t1+" = phi i1 [ false, %"+l1 +"], [ "+reg2+", %"+l2+" ]\n\n");

        return t1+":AND:i1:boolean";
    }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "<"
  * f2 -> PrimaryExpression()
  */
 public String visit(CompareExpression n, String argu) throws Exception {
    String prima_L = n.f0.accept(this, argu);
    String prima_R = n.f2.accept(this, argu);

    // get the registers
    String reg1 = prima_L.split(":")[0];
    String reg2 = prima_R.split(":")[0];

    String t1 = register();

    // compare with icmp
    emit("\t"+t1+" = icmp slt i32 "+reg1+", "+reg2+"\n\n");

    return t1+":LESS:i1:boolean";

 }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "+"
  * f2 -> PrimaryExpression()
  */
 public String visit(PlusExpression n, String argu) throws Exception {
    String prima_L = n.f0.accept(this, argu);
    String prima_R = n.f2.accept(this, argu);

    // get the registers
    String reg1 = prima_L.split(":")[0];
    String reg2 = prima_R.split(":")[0];

    String t1 = register();

    // add them
    emit("\t"+t1+" = add i32 "+reg1+", "+reg2+"\n\n");

    return t1+":PLUS:i32:int";

 }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "-"
  * f2 -> PrimaryExpression()
  */
 public String visit(MinusExpression n, String argu) throws Exception {
    String prima_L = n.f0.accept(this, argu);
    String prima_R = n.f2.accept(this, argu);

    // get the registers
    String reg1 = prima_L.split(":")[0];
    String reg2 = prima_R.split(":")[0];

    String t1 = register();

    // subtract
    emit("\t"+t1+" = sub i32 "+reg1+", "+reg2+"\n\n");

    return t1+":MINUS:i32:int";

 }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "*"
  * f2 -> PrimaryExpression()
  */
 public String visit(TimesExpression n, String argu) throws Exception {
    String prima_L = n.f0.accept(this, argu);
    String prima_R = n.f2.accept(this, argu);
    String reg1 = prima_L.split(":")[0];
    String reg2 = prima_R.split(":")[0];

    String t1 = register();

    // multiply with mul
    emit("\t"+t1+" = mul i32 "+reg1+", "+reg2+"\n\n");

    return t1+":MULTI:i32:int";
 }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "["
  * f2 -> PrimaryExpression()
  * f3 -> "]"
  */
 public String visit(ArrayLookup n, String argu) throws Exception {
    String prima_L = n.f0.accept(this, argu);
    String prima_R = n.f2.accept(this, argu);
    
    String t1 = register();
    String t2 = register();
    String t3 = register();
    String t4 = register();
    String t5 = register();
    String t6 = register();

    String reg = prima_L.split(":")[0];
    String indx = prima_R.split(":")[0];    

    String l1 = label();
    String l2 = label();
    String l3 = label();
    String l4 = label();    


    if(prima_L.split(":")[2].equals("%_IntegerArray*")){

        // negative check >=0
        emit("\t"+t1+" = icmp sge i32 "+indx+", 0\n");
        emit("\tbr i1 "+t1+", label %"+l2+", label %"+l1+"\n\n");

        // label l1 negative
        emit(l1+":\n");
        emit("\tcall void @throw_oob()\n");
        
        emit("\tbr label %"+l2+"\n\n");

        // LABEL 2 its not
        emit(l2+":\n");

        // first struct element
        emit("\t"+t2+" = getelementptr %_IntegerArray, %_IntegerArray* "+reg+", i32 0, i32 0 \n");

        // t3 identifier array size
        emit("\t"+t3+" = load i32, i32* "+t2+"\n\n");

        // check index < array size
        emit("\t"+t4+" = icmp slt i32 "+indx+", "+ t3+"\n");
        emit("\tbr i1 "+t4+", label %"+l4+ ", label %"+l3 +"\n\n");

        // LABEL 3 out of bounds 
        emit(l3+":\n");
        emit("\tcall void @throw_oob()\n\tbr label %"+l4+"\n\n");

        // LABEL 4 in bounds
        emit(l4+":\n");
            
        // find it and load
        emit("\t"+t5 + " = getelementptr %_IntegerArray, %_IntegerArray* "+reg+ ", i32 0, i32 1, i32 "+indx+"\n\n");
        emit("\t"+t6+ " = load i32, i32* "+t5+"\n\n");

    }
    else{

        // negative check >=0
        emit("\t"+t1+" = icmp sge i32 "+indx+", 0\n");
        emit("\tbr i1 "+t1+", label %"+l2+ ", label %"+l1 +"\n\n");

        // label l1 its negative
        emit(l1+":\n");
        emit("\tcall void @throw_oob()\n\tbr label %"+l2+"\n\n");

        // LABEL 2 its not
        emit(l2+":\n");

        // first struct element
        emit("\t"+t2+" = getelementptr %_BooleanArray, %_BooleanArray* "+reg+", i32 0, i32 0 \n");

        // t3 identifier array size
        emit("\t"+t3+" = load i32, i32* "+t2+"\n\n");

        // check index < array size
        emit("\t"+t4+" = icmp slt i32 "+indx+", "+ t3+"\n");
        emit("\tbr i1 "+t4+", label %"+l4+ ", label %"+l3 +"\n\n");

        // LABEL 3 out of bounds 
        emit(l3+":\n");
        emit("\tcall void @throw_oob()\n\tbr label %"+l4+"\n\n");

        // LABEL 4 in bounds
        emit(l4+":\n");

        // find and load
        emit("\t"+t5 + " = getelementptr %_BooleanArray, %_BooleanArray* "+reg+ ", i32 0, i32 1, i32 "+indx+"\n\n");
        emit("\t"+t6+ " = load i1, i1* "+t5+"\n\n");

    }

    return t6+":LOOKUP:i32:int";
 }

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> "length"
  */
 public String visit(ArrayLength n, String argu) throws Exception {
    String _ret = n.f0.accept(this, argu);
    String myREG= _ret.split(":")[0];
    String myType= _ret.split(":")[2];
    String t1=register();
    String t2=register();

    if (myType.equals("%_BooleanArray*")){
        emit("\t"+t1 + " = getelementptr %_BooleanArray, %_BooleanArray* "+myREG+ ", i32 0, i32 0 \n\n");
        emit("\t"+t2+ " = load i32, i32* "+t1+"\n\n");
    }
    else{
        emit("\t"+t1 + " = getelementptr %_IntegerArray, %_IntegerArray* "+myREG+ ", i32 0, i32 0 \n\n");
        emit("\t"+t2+ " = load i32, i32* "+t1+"\n\n");
    }

    return t2+":LENGTH:i32:int";
}

 /**
  * f0 -> PrimaryExpression()
  * f1 -> "."
  * f2 -> Identifier()
  * f3 -> "("
  * f4 -> ( ExpressionList() )?
  * f5 -> ")"
  */
 public String visit(MessageSend n, String argu) throws Exception {
    String prima = n.f0.accept(this, argu);
    String reg1 = prima.split(":")[0];
    String class_name = prima.split(":")[3];

    // to get the actual method name
    String method_name = n.f2.accept(this, "business_as_usual"); 

    // to get the arguments
    String[] arguments = n.f4.present()? n.f4.accept(this, argu).split(",") : new String[0];

    // registers that will be required
    String t1 = register();
    String t2 = register();
    String t3 = register();
    String t4 = register();
    String t5 = register();
    String t6 = register();

    // bitcast to i8*** and load
    emit("\t"+t1+ " = bitcast i8* "+reg1+" to i8***\n");
    emit("\t"+t2+ " = load i8**, i8*** "+ t1+" \n");

    // get the right class name from the virtual table
    String who_do_i_belong_to = v_taburu.lost_and_found(class_name, method_name);
    String meth_type = taburu.get_class(who_do_i_belong_to).get_method_type(method_name);

    // get the relative offset for getelementptr
    int offset = taburu.relative_offset(who_do_i_belong_to, method_name);
    
    emit("\t"+t3+ " = getelementptr i8*, i8** "+ t2+", i32 "+offset+" \n");
    emit("\t"+t4+ " = load i8*, i8** "+t3+"\n");

    emit("\t"+t5+" = bitcast i8* "+t4+" to ");
    emitType(meth_type);
    emit(" (i8*");

    // get the types from the symbol table
    for(int i=0; i< taburu.get_class(who_do_i_belong_to).get_arg_size(method_name); i++){
        emit(", ");
        emitType(taburu.get_class(who_do_i_belong_to).get_arg_type(method_name, i));
    }
    emit(")*\n\n");

    emit("\t"+t6+" = call ");
    emitType(meth_type);
    emit(" "+t5+"(i8* "+reg1);

    // get the argument types from the symbol table
    for(int i=0; i< taburu.get_class(who_do_i_belong_to).get_arg_size(method_name); i++){
        emit(", ");
        emitType(taburu.get_class(who_do_i_belong_to).get_arg_type(method_name, i));
        emit(" "+arguments[i].split(":")[0]);
    }
    emit(")\n\n");

    String lvmh = MJtoLL(meth_type);

    return t6+":MSEND:"+lvmh+":"+meth_type;
 }

}