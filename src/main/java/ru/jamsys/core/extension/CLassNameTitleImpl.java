package ru.jamsys.core.extension;

public class CLassNameTitleImpl implements CLassNameTitle {

    public static String getClassNameTitleStatic(Class<?> cls, String index) {
        return new CLassNameTitleImpl().getClassNameTitle(cls, index);
    }

}
