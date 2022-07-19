# MiniJava to LLVM Compiler

This is a MiniJava to LLVM compiler written in Java. It performs semantic analysis on the given files and then translates them into intermediate code. MiniJava is a subset of Java, while LLVM IR can be compiled with clang.

The purpose of this project aside from building the compiler itself, was familiarizing myself with the visitor pattern. By writing visitors that perform the type checking and compilation, allows the existing structure of JTB to remain intact.



## Implementation

In my implementation I chose to have two visitors that gather information for the Symbol Table and Virtual Table respectively, then one to do the type checking and finally one to translate the MiniJava programs to the LLVM intermediate code. All visitors are subclasses of GJDepthFirst.

### Collector

In [Collector.java](Collector.java) is the first visitor that collects in the Symbol Table the names of the classes, variables and methods, as well as their types. All this is done using the functions of the Symbol Table. This way the error handling is done internally.

### Typechecker

In [Typechecker.java](Typechecker.java) is the visitor that performs the semantic analysis by making a second "pass" of the code. Here, I changed the implementation of the visitor functions to return the types of variables or methods. Also, in some of them, I pass as a second argument the scope information needed as a String. 

### Gatherer

In [Gatherer.java](Gatherer.java) is the visitor that gathers in the Virtual Table the names of the classes, methods and the class they belong to, in case of overridden methods. So here visiting only the class and method declarations is enough, since the Symbol Table already holds the rest of the information needed for the translation.

### Translator

In [Translator.java](Translator.java) is the final visitor that converts the MiniJava code into the intermediate representation used by the LLVM compiler. Here, in each visit function the corresponding LLVM code is "emitted" to the .ll file that is created. To represent the arrays I chose to use the Struct Type class:
```
%_BooleanArray = type { i32, [0 x i1] }
%_IntegerArray = type { i32, [0 x i32] }
```
In this way, by storing the size of the array in the first position, it is easier to perform the in bounds checks. For the types and offsets, I utilize the Symbol and Virtual Tables to retrieve the necessary information.

## Symbol Table and Virtual Table

In [Basket_Full_Of_Stuff.java](Basket_Full_Of_Stuff.java) are the classes that hold the scope and type information to perform the static checking, i.e. the Symbol Table, and to determine the inheritance of the methods, i.e. the Virtual Table. I used Hash Maps to keep track of the class declarations and method variables, while in the cases that the order of appearance in the code is important, e.g. for the offsets, I used Linked Hash Maps. Thus, I have the following Symbol Table structure:

| Data Structure | Values |
| ----------- | ----------- |
| Hash Map | Classes |
| Linked Hash Map | Class Variables |
| Linked Hash Map | Class Methods |
| Hash Map | Method Variables |
| Linked Hash Map | Method Arguments |

For the Virtual Table, where the method order is important since it is used to generate the LLVM intermediate code I used Linked Hash Maps, which are nested. So I have the following Virtual Table structure:

| Data Structure | Values |
| ----------- | ----------- |
| Linked Hash Map | Classes |
| Linked Hash Map | Class Methods |

## Main

The Main class in [Main.java](Main.java) runs the semantic analysis, initiating the parser that was produced by JavaCC and executing the visitors I described above. When the semantic check has finished, for every class of the program the names and the offsets of every field and method this class contains are printed. Finally, the Translator visitor compiles the program into LLVM which is emitted into the .ll file, that is created in the same folder as the original .java file.

## Compilation and Execution

To compile all the files:
``` 
$ make compile 
```

To semantically check and translate everything in the Files directory:
``` 
$ make all
```

To translate specific MiniJava files they can also be given as arguments: 
``` 
$ java Main [file1] [file2] ... [fileN] 
```

To delete all the class files, as well as the syntax tree:
``` 
$ make clean
```
 
### Acknowledgements

This project was an assignment for the [Compilers](https://cgi.di.uoa.gr/~compilers/) course of Spring 2022 taught by [Yannis Smaragdakis](https://yanniss.github.io).

Further information on the LLVM language is documented in the [LLVM Reference Manual](https://llvm.org/docs/LangRef).