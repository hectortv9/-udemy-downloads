package com.hectortv9.tools;

import java.util.Scanner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SomeUtilities {

    public static void main(String[] args) {
        varToConstant();
    }
    
    public static void varToConstant() {
        Scanner in = new Scanner(System.in);        
        
        //Use <Control+Z> or <Control+D> to terminate
        while(in.hasNext()){
            String varName = in.next(); // Use in.nextLine() for line-by-line reading
            String constantName =
                    varName
                    .codePoints()
                    .mapToObj(ch -> Character.isLetter(ch) && Character.isLowerCase(ch) ? 
                        String.valueOf(Character.toChars(Character.toUpperCase(ch))) :
                        "_".concat(String.valueOf(Character.toChars(ch))))
                    .collect(Collectors.joining());
                System.out.println(constantName);
        }

    }

}
