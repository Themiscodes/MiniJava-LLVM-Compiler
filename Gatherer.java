import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.*;

class Gatherer extends GJDepthFirst<String, String>{

    private Taburu taburu;
    private V_Taburu v_taburu;

    public Gatherer(Taburu symboru, V_Taburu v_taburu1){
        this.taburu = symboru;
        this.v_taburu = v_taburu1;
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
        String class_name = n.f1.accept(this, null);

        // gather the class
        v_taburu.gather_class(class_name);

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
        String class_name = n.f1.accept(this, null);

        v_taburu.gather_class(class_name);

        // add the class name for the method
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
        String superclass_name = n.f3.accept(this, null);

        v_taburu.gather_class(class_name);

        // first copy the superclass virtual table
        for (Map.Entry<String,String> entry : v_taburu.get_virtual_table(superclass_name).entrySet()) {
            v_taburu.gather_method(entry.getKey(), class_name, superclass_name, taburu);
        }

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
        String my_name = n.f2.accept(this, null);

        // if here then method of the class of argu
        v_taburu.gather_method(my_name, argu, argu, taburu);

        return null;
    }

    // Also need the Identifier visitor to get the strings
    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }

}
