import java.util.*;
import java.io.*;

// information of a class
public class Basket_Full_Of_Stuff{

    private String class_name;
    private String parent;
    private int var_offset;
    private int meth_offset;

    // linked hash maps to keep the order of the offsets
    private Map<String, Variable> my_variables = new LinkedHashMap<String, Variable>();
    private Map<String, Method> my_methods = new LinkedHashMap<String, Method> ();

    // the constructor
    public Basket_Full_Of_Stuff(String class_name, String parent, int var_offset, int meth_offset) {
        this.class_name = class_name;
        this.parent = parent;
        this.var_offset = var_offset;
        this.meth_offset = meth_offset;
    }

    // getters and Setters
    public String getClass_name() {
        return class_name;
    }

    public void setClass_name(String class_name) {
        this.class_name = class_name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public int getVar_offset() {
        return var_offset;
    }

    public void setVar_offset(int var_offset) {
        this.var_offset = var_offset;
    }

    public int getMeth_offset() {
        return meth_offset;
    }

    public void setMeth_offset(int meth_offset) {
        this.meth_offset = meth_offset;
    }

    public Variable get_variable(String name) {
        if (this.my_variables.containsKey(name)) {
            return this.my_variables.get(name);
        }
        return null;
    }

    public Method get_method(String name) {
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name);
        }
        return null;
    }

    public int get_method_offset(String name) {
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name).getOffset();
        }
        return -1;
    }

    public String get_method_type(String name) {
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name).getType();
        }
        return "";
    }

    public int get_arg_size(String name){
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name).getArgument_num();
        }
        return 0;
    }

    public String get_arg_type(String name, int i){
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name).getArgument(i).getType();
        }
        return "";
    }

    public String get_arg_name(String name, int i){
        if (this.my_methods.containsKey(name)) {
            return this.my_methods.get(name).getArgument(i).getName();
        }
        return "";
    }

    // insert a method in the map
    public void insert_method(Method method) throws Exception {

        // check if it has already been declared
        if (this.my_methods.containsKey(method.getName())) {
            throw new Exception("Method: " + method.getName() + " has already been declared.");
        }

        // check for overloading
        if(method.getOffset()!=-1){

            // check that it is not the main
            if (!method.getName().equals("main")) {
                method.setOffset(this.meth_offset);
                this.meth_offset += 8;
            }

        }

        my_methods.put(method.getName(), method);

    }


    // insert a variable in the map
    public void insert_variable(Variable variable) throws Exception{

        // check if it has already been declared
        if (this.my_variables.containsKey(variable.getName())){
            throw new Exception("Variable: "+ variable.getName()+" has already been declared.");
        }

        // add the offset
        variable.setOffset(this.var_offset);
        my_variables.put(variable.getName(), variable);

        // depending on the type also add on the offset
        switch(variable.getType()){
            case "int":
                this.var_offset+=4;
                break;
            case "boolean":
                this.var_offset+=1;
                break;
            default:
                this.var_offset+=8;
                break;
        }

    }

    public void print_variable_offsets(){
        
        for (Map.Entry<String, Variable> entry : this.my_variables.entrySet()){
            System.out.println(this.getClass_name() +"."+entry.getValue().getName()+ " : " + entry.getValue().getOffset());
        }

    }

    public void print_method_offsets(){
        
        for (Map.Entry<String, Method> entry : this.my_methods.entrySet()){
            
            // if it is not overloaded
            if ( entry.getValue().getOffset()!=-1)
                System.out.println(this.getClass_name() +"."+entry.getValue().getName()+ " : " + entry.getValue().getOffset());
        
        }

    }

    // simple function to check if it is the main class or not
    public boolean im_not_main(){

        if (this.my_methods.containsKey("main")) {
            return false;
        }
        
        return true;
    }

}


// information of a variable
class Variable{

    private String name;
    private String type;
    private int offset;

    public Variable(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}


// information of a method
class Method {

    private String name;
    private String type;
    private String scope;
    private int offset;
    private int argument_num;

    // LinkedHashMap to keep the order of its arguments
    private Map <Integer, Variable> arguments = new LinkedHashMap<Integer, Variable>();
    private Map <String, Variable> variables = new HashMap<String, Variable> ();

    // the constructor
    public Method(String class_name, String name, String type, String argument_list) {

        this.name = name;
        this.type = type;
        this.scope = class_name;

        // initially 0 to switch to -1 if needed
        this.offset = 0; 

        // add the arguments if they exist
        if (argument_list!="") {

            // kano me to keno gia na min menei stin arxi toy typou
            String[] da_argz = argument_list.split(", ");
            
            this.argument_num=da_argz.length;

            for(int index=0;index<da_argz.length;index++){
                Variable var = new Variable(da_argz[index].split(" ")[1], da_argz[index].split(" ")[0]);
                arguments.put(index, var);
            }

        }

    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public int getArgument_num() {
        return argument_num;
    }

    // get argument searching with index
    public Variable getArgument(int i){
        return this.arguments.get(i);
    }

    // get argument searching with name
    public Variable get_argument(String name){
        for (int i=0;i<this.argument_num;i++){
            if(this.getArgument(i).getName().equals(name)){
                return this.getArgument(i);
            }
        }
        return null;
    }

    // get variable searching with name
    public Variable get_variable(String name){
        if (this.variables.containsKey(name))
            return this.variables.get(name);
        return null;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    // overloarding method error check
    public void how_do_we_compare(Method madness) throws Exception{

        // check the types
        if (this.type != madness.getType()){
            throw new Exception("Wrong type: " + madness.getType() + " expected: "+this.type );
        }

        // check the arguments number
        if (this.argument_num != madness.getArgument_num()){
            throw new Exception("Wrong number of arguments.");
        }

        // check the argument types
        for (int i=0;i<this.argument_num;i++){
            if(!this.getArgument(i).getType().equals(madness.getArgument(i).getType())){
                throw new Exception("Wrong type: "+madness.getType() + madness.getArgument(i).getType()+" expected: "+this.getArgument(i).getType());
            }
        }

    }

    // insert variable in map
    public void insert_variable(Variable variable) throws Exception{

        // check if it's already declared
        if (this.variables.containsKey(variable.getName())){
            throw new Exception("A variable with the same name has already been declared.");
        }

        // same for the arguments
        for (int i=0;i<this.argument_num;i++){
            if(this.getArgument(i).getName().equals(variable.getName())){
                throw new Exception("An argument with the same name " +variable.getName()+ " has been declared.");
            }
        }

        this.variables.put(variable.getName(), variable);

    }

}


// the Symbol Table with all the classes, variables and methods
class Taburu{

    // HashMap of classes
    private Map<String, Basket_Full_Of_Stuff> the_classes = new HashMap<String, Basket_Full_Of_Stuff> ();

    // insert class in the map
    public void collect_class(String class_name, String mama_class) throws Exception{

        // check if the class has already been declared
        if (the_classes.containsKey(class_name)){
            throw new Exception("Class Name: "+class_name+" is already declared.");
        }

        // if it is an extend check that the superclass exists
        if (mama_class!=null && !the_classes.containsKey(mama_class)){
            throw new Exception("Super Class: "+mama_class+" has not been declared.");
        }

        // initialise Basket_Full_Of_Stuff class
        Basket_Full_Of_Stuff information = new Basket_Full_Of_Stuff(class_name, null, 0, 0);

        // if there is a superclass add its information as well
        if (mama_class!=null){

            Basket_Full_Of_Stuff hold_this_for_a_sec = the_classes.get(mama_class);
            information.setParent(mama_class);
            information.setVar_offset(hold_this_for_a_sec.getVar_offset());
            information.setMeth_offset(hold_this_for_a_sec.getMeth_offset());
        
        }

        // insert in the map
        the_classes.put(class_name, information);

    }

    // insert a method
    public void collect_method(String class_name, String method_name, String type, String argument_list) throws Exception {

        Method my_method = new Method(class_name, method_name, type, argument_list);

        // check the super classes
        String iter = class_name;
        while (this.the_classes.get(iter).getParent()!=null){

            // iterator on the super class
            iter = this.the_classes.get(iter).getParent();

            // is this method in the map
            if(this.the_classes.get(iter).get_method(method_name)!=null){

                Method temp = this.the_classes.get(iter).get_method(method_name);

                // if this function doesn't throw an error, then no problem
                temp.how_do_we_compare(my_method);

                // -1 meaning override
                my_method.setOffset(-1);

            }
        }

        this.the_classes.get(class_name).insert_method(my_method);
    }

    // insert variable in the scope of the method or class that it belongs
    public void collect_variable(String my_name, String my_type, String who_do_i_belong_to) throws Exception {

        if(who_do_i_belong_to==null){
            throw new Exception("Mistake");
        }

        Variable my_variable = new Variable(my_name, my_type);

        // check if it belongs to a method or a class
        if (who_do_i_belong_to.contains(":imamethodof:")){

            String[] what_is_what = who_do_i_belong_to.split(":imamethodof:");
            this.the_classes.get(what_is_what[1]).get_method(what_is_what[0]).insert_variable(my_variable);
        
        }
        else{
        
            this.the_classes.get(who_do_i_belong_to).insert_variable(my_variable);
        
        }

    }

    public Basket_Full_Of_Stuff get_class (String class_name){
        if (this.the_classes.containsKey(class_name)){
            return this.the_classes.get(class_name);
        }
        return null;
    }

    // returns the type of the variable
    public String find_me(String variable_name, String who_do_i_belong_to) throws Exception{

        if (who_do_i_belong_to==null) {

            // new Class() case
            if (this.get_class(variable_name)!=null) 
                return variable_name;
            else
                throw new Exception(variable_name + " has not been defined.");

        }

        // check if it belongs to a method or a class
        if (who_do_i_belong_to.contains(":imamethodof:")){

            String[] what_is_what = who_do_i_belong_to.split(":imamethodof:");

            if (this.get_class(what_is_what[1])!=null && this.get_class(what_is_what[1]).get_method(what_is_what[0])!=null) {
                Variable varvar = this.get_class(what_is_what[1]).get_method(what_is_what[0]).get_variable(variable_name);
                if (varvar != null) {
                    return varvar.getType();
                }

                varvar = this.get_class(what_is_what[1]).get_method(what_is_what[0]).get_argument(variable_name);
                if (varvar != null) {
                    return varvar.getType();
                }
            }

            // check the fields of the class
            who_do_i_belong_to = what_is_what[1];

        }

        if (this.get_class(who_do_i_belong_to)!=null){

            Variable varvar = this.get_class(who_do_i_belong_to).get_variable(variable_name);
            if (varvar!=null){
                return varvar.getType();
            }

            // if it's not there recursively call for the super class
            return this.find_me(variable_name, this.get_class(who_do_i_belong_to).getParent());
        }

        // if it reached this point there the variable isn't defined
        throw new Exception(variable_name+" has not been defined.");

    }

    // returns the type of the method
    public String call_me(String my_name, String my_class, String[] my_arguments) throws Exception {

        // check if the class exists
        Basket_Full_Of_Stuff object = this.get_class(my_class);
        if (object==null)
            throw new Exception("Class of this type doesn't exist.");

        // check if the method exists in the class or one of the super classes
        while (object!=null&&object.get_method(my_name)==null){
            object = this.get_class(object.getParent());
        }

        // if object is present then it's been found
        if(object!=null){

            Method my_method = object.get_method(my_name);

            // check the arguments
            if (my_arguments.length == my_method.getArgument_num()){
                for (int i =0; i< my_arguments.length; i++){
                    if (whats_our_type(my_method.getArgument(i).getType(), my_arguments[i])==null){
                      throw new Exception("Incompatible type of argument in method: "+ my_name);
                    }
                }
                return my_method.getType();
            }
            else{
                throw new Exception("Wrong number of arguments.");
            }

        }

        // if object is null then it hasn't been found
        throw new Exception("Method doesn't exist: " + my_name);

    }

    // check left and right type
    public String whats_our_type(String type_L, String type_R) throws Exception{

        // check if they're one of the basic four types
        if (type_L.equals("int")&&type_R.equals("int")){
            return "int";
        }
        if (type_L.equals("int[]")&&type_R.equals("int[]")){
            return "int[]";
        }
        if (type_L.equals("boolean")&&type_R.equals("boolean")){
            return "boolean";
        }
        if (type_L.equals("boolean[]")&&type_R.equals("boolean[]")){
            return "boolean[]";
        }

        // check if it is a class name
        if (this.get_class(type_L)!=null && this.get_class(type_R)!=null) {

            // if they're the same
            if (type_L.equals(type_R)) {

                return type_R;
            }

            // check for the super class
            type_R = this.get_class(type_R).getParent();

            // BE CAREFUL the opposite isn't true (since assignment or return method)
            while (type_R!=null){
                if (type_L.equals(type_R)) {
                    return type_R;
                }
                type_R = this.get_class(type_R).getParent();
            }
        }

        // if this point is reached then there is an error
        throw new Exception("Wrong types: "+ type_L+" - "+type_R);

    }


     // return LLVM offset
     public int find_my_offset(String variable_name, String who_do_i_belong_to) {

        if (who_do_i_belong_to==null) {
            return -1;
        }
        
        // check if it's a method or class variable
        if (who_do_i_belong_to.contains(":imamethodof:")){

            String[] what_is_what = who_do_i_belong_to.split(":imamethodof:");

            if (this.get_class(what_is_what[1])!=null && this.get_class(what_is_what[1]).get_method(what_is_what[0])!=null) {
                Variable varvar = this.get_class(what_is_what[1]).get_method(what_is_what[0]).get_variable(variable_name);
                if (varvar != null) {
                    return -1;
                }

                varvar = this.get_class(what_is_what[1]).get_method(what_is_what[0]).get_argument(variable_name);
                if (varvar != null) {
                    return -1;
                }
            }

            // check the class fields
            who_do_i_belong_to = what_is_what[1];
        }

        if (this.get_class(who_do_i_belong_to)!=null){

            Variable varvar = this.get_class(who_do_i_belong_to).get_variable(variable_name);
            if (varvar!=null){
                return varvar.getOffset()+8;
            }

            // recursively call for the super class
            return this.find_my_offset(variable_name, this.get_class(who_do_i_belong_to).getParent());
        }

        // it hasn't been found
        return -1;
    }

    // get the relative offset for LLVM
    public int relative_offset(String class_name, String method_name){

        int my_offset=0;

        // if it's -1 (overriden offset) go "upwards" to the superclass
        while(this.get_class(class_name).get_method_offset(method_name)==-1){
            class_name= this.get_class(class_name).getParent();
        }

        my_offset = this.get_class(class_name).get_method_offset(method_name);
        
        // since it's relative for LLVM divide by 8
        return my_offset/8;
    }

    // to print the class offsets
    public void print_class_offsets(){

        // for every class
        for (Map.Entry<String, Basket_Full_Of_Stuff> entry : the_classes.entrySet()){

            // if it's not the main class
            if (entry.getValue().im_not_main()) {
                System.out.println("------ Class "+ entry.getValue().getClass_name() + " ------");
                System.out.println("-- Variables --");
                entry.getValue().print_variable_offsets();
                System.out.println("--  Methods  --");
                entry.getValue().print_method_offsets();
            }

        }

    }

}


// the Virtual Table
class V_Taburu {

    // LinkedHashMap to retain the right order
    private Map<String, LinkedHashMap<String, String>> virtual_table = new LinkedHashMap<String, LinkedHashMap<String, String> > ();

    // insert class in the virtual table
    public void gather_class(String class_name) throws Exception{

        if (virtual_table.containsKey(class_name))
            throw new Exception("A class with the same name exists already in the vtable.");

        virtual_table.put(class_name, new LinkedHashMap<String, String>() );

    }

    // insert method in the virtual table
    public void gather_method(String method_name, String class_name, String inheritance, Taburu taburu) throws Exception{
        
        if (!virtual_table.containsKey(class_name))
            throw new Exception("Method's class does not exist in the vtable.");

        // if a method is overriden simply change the inheritance
        if (virtual_table.get(class_name).containsKey(method_name))
            virtual_table.get(class_name).replace(method_name, inheritance);
        else{

            if(class_name.equals(inheritance)){
                virtual_table.get(class_name).put(method_name, inheritance);
            }
            else{
                // find who it inherits from
                while(!virtual_table.get(inheritance).containsKey(method_name)){
                    inheritance= taburu.get_class(inheritance).getParent();
                }
                virtual_table.get(class_name).put(method_name, virtual_table.get(inheritance).get(method_name));
            }
        }

    }

    // return all classes in a list
    public List<String> get_all_classes() {
        List<String> all_my_classes = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, String>> entry : virtual_table.entrySet()) {
            all_my_classes.add(entry.getKey());
        }
        return all_my_classes;
    }

    // returns the method count, ie the size of the map
    public int get_method_count(String classname){
        return virtual_table.get(classname).size();
    }

    // find the inheritance of a method
    public String lost_and_found(String class_name, String method_name){

        if(virtual_table.containsKey(class_name)){
            if(virtual_table.get(class_name).containsKey(method_name)){
                return virtual_table.get(class_name).get(method_name);
            }
        }
        return "Error";
    
    }
    
    // return the virtual table, ie the linked hash map
    public LinkedHashMap<String, String> get_virtual_table(String class_name){

        // if this class exists
        if (virtual_table.containsKey(class_name))
            return virtual_table.get(class_name);

        // else returns null
        return null;

    }

}