package org.fedoraproject.javapackages.validator;

public class Main {
    public static void main(String[] args) throws Exception {
        BytecodeVersionCheck.main(args);
        FilepathsCheck.main(args);
        RpmAttributeCheck.main(args);
        RpmFilesizeCheck.main(args);
    }
}
