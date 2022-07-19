import visitor.GJDepthFirst;
import syntaxtree.*;

public class Typechecker extends GJDepthFirst<String, String>{

   private Taburu taburu;
   
   public Typechecker(Taburu symboru){
      this.taburu = symboru;
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
   public String visit(MainClass n, String argu) throws Exception {
      String class_name = n.f1.accept(this, ":businessasusual:");

      // checking the statements of main is enough
      n.f15.accept(this, "main:imamethodof:"+class_name);

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
   public String visit(ClassDeclaration n, String argu) throws Exception {
      String class_name = n.f1.accept(this, ":businessasusual:");

      // pass the class_name as well
      n.f4.accept(this,  class_name);

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
   public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
      String class_name = n.f1.accept(this, ":businessasusual:");

      // since superclass has been collected simply check method decls
      n.f6.accept(this,  class_name);

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
   public String visit(MethodDeclaration n, String argu) throws Exception {

      // to check the return type is correct
      String type_L = n.f1.accept(this, null);
      String my_name = n.f2.accept(this, ":businessasusual:");
      String type_R = n.f10.accept(this, my_name + ":imamethodof:"+ argu);

      // check method statements
      n.f8.accept(this, my_name + ":imamethodof:" + argu);

      // throws Exception internally if there's an error
      return taburu.whats_our_type(type_L, type_R);

   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, String argu) throws Exception {
      String type_L = n.f0.accept(this, argu);
      String type_R = n.f2.accept(this, argu);

      // whats_our_type returns their type or exception if they mismatch
      return taburu.whats_our_type(type_L, type_R);

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
      String array_type = n.f0.accept(this, argu);
      String array_index = n.f2.accept(this, argu);
      String assgnmt_type = n.f5.accept(this, argu);

      // check that index is an int
      if (!array_index.equals("int"))
         throw new Exception("Index of array has to be an integer.");

      // integer array case
      if (array_type.equals("int[]")&&assgnmt_type.equals("int")) {
         return "int[]";
      }

      // boolean array
      if (array_type.equals("boolean[]")&&assgnmt_type.equals("boolean")) {
         return "boolean[]";
      }

      // wrong type if we reached this point
      throw new Exception("Wrong array assignment types.");

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
      String _ret= n.f2.accept(this, argu);

      
      if (_ret.equals("boolean")) {
         n.f4.accept(this, argu);
         n.f6.accept(this, argu);
         return _ret;
      }

      throw new Exception("Condition isn't boolean.");
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, String argu) throws Exception {
      String _ret= n.f2.accept(this, argu);

      // check that condition is boolean
      if (_ret.equals("boolean")) {
         n.f4.accept(this, argu);
         return _ret;
      }

      throw new Exception("Condition isn't boolean.");
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

      // this is based on the project's outline that print accepts ints only
      if (_ret.equals("int")) return _ret;

      throw new Exception("Println accepts only integers.");
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

   public String visit(BooleanArrayType n, String argu) {
      return "boolean[]";
   }

   public String visit(IntegerArrayType n, String argu) {
      return "int[]";
   }

   public String visit(BooleanType n, String argu) {
      return "boolean";
   }

   public String visit(IntegerType n, String argu) {
      return "int";
   }

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, String argu) throws Exception {
      String clause_L = n.f0.accept(this, argu);
      String clause_R = n.f2.accept(this, argu);

      if (clause_L.equals("boolean")&&clause_R.equals("boolean"))
         return "boolean";

      throw new Exception("And expression accepts boolean clauses.");
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu) throws Exception {
      String prima_L = n.f0.accept(this, argu);
      String prima_R = n.f2.accept(this, argu);

      if (prima_L.equals("int")&&prima_R.equals("int"))
         return "boolean";

      throw new Exception("Comparison isn't between integers.");

   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu) throws Exception {
      String prima_L = n.f0.accept(this, argu);
      String prima_R = n.f2.accept(this, argu);


      if (prima_L.equals("int")&&prima_R.equals("int"))
         return "int";

      throw new Exception("Addition isn't between integers.");

   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu) throws Exception {
      String prima_L = n.f0.accept(this, argu);
      String prima_R = n.f2.accept(this, argu);


      if (prima_L.equals("int")&&prima_R.equals("int"))
         return "int";

      throw new Exception("Subtraction isn't between integers.");
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu) throws Exception {
      String prima_L = n.f0.accept(this, argu);
      String prima_R = n.f2.accept(this, argu);

      if (prima_L.equals("int")&&prima_R.equals("int"))
         return "int";

      throw new Exception("Multiplication isn't between integers.");
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

      if (!prima_R.equals("int"))
         throw new Exception("Array index isn't an integer.");

      if (prima_L.equals("int[]"))
         return "int";

      if (prima_L.equals("boolean[]"))
         return "boolean";

      throw new Exception("Wrong type, expected array.");
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu) throws Exception {
      String _ret = n.f0.accept(this, argu);


      if (_ret.equals("int[]"))
         return "int";

      if (_ret.equals("boolean[]"))
         return "int";

      throw new Exception("Wrong type, expected array.");
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
      String object = n.f0.accept(this, argu);

      // business as usual to get the actual string
      String method = n.f2.accept(this, ":businessasusual:");
      String[] arguments = n.f4.present()? n.f4.accept(this, argu).split(",") : new String[0];

      // return type if they're correct otherwise throws exception
      return taburu.call_me(method, object, arguments);
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
   
   // to handle the above cases:

   /**
    * f0 -> "INTEGER_LITERAL"
    */
   public String visit(IntegerLiteral n, String argu) throws Exception {
      return "int";
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, String argu) throws Exception {
      return "boolean";
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, String argu) throws Exception {
      return "boolean";
   }

   /**
    * f0 -> IDENTIFIER
    */
   public String visit(Identifier n, String argu) throws Exception {
      String identifier =n.f0.toString();

      // so that it works by returning the string
      if (argu==null||argu.equals(":businessasusual:"))
         return identifier;

      // else return the type with find_me function or throw exception
      return taburu.find_me(identifier, argu);
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, String argu) throws Exception {
      String[] bettersafethansorry = argu.split(":imamethodof:");
      return bettersafethansorry[1];
   }

   /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
      String _ret = n.f3.accept(this, argu);
      if (_ret.equals("int"))  return "boolean[]";
      throw new Exception("Index of array isn't int.");
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
      // apla elegxos an to expression einai ontos int
      String _ret = n.f3.accept(this, argu);
      if (_ret.equals("int"))  return "int[]";
      throw new Exception("Index of array isn't int.");
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String argu) throws Exception {
      String _ret = n.f1.accept(this, argu);
      if(taburu.get_class(_ret)!=null){
         return _ret;
      }
      throw new Exception("This class doesn't exist.");
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu) throws Exception {
      String _ret = n.f1.accept(this, argu);
      if (_ret.equals("boolean")) {
         return "boolean";
      }
      throw new Exception("Expected boolean.");
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

}

