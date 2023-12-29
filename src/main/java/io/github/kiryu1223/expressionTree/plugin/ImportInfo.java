package io.github.kiryu1223.expressionTree.plugin;

public class ImportInfo
{
    private final String name;
    private  final boolean isStatic;

    public ImportInfo(String name, boolean isStatic)
    {
        this.name = name;
        this.isStatic = isStatic;
    }

    public String getName()
    {
        return name;
    }

    public boolean isStatic()
    {
        return isStatic;
    }
}
