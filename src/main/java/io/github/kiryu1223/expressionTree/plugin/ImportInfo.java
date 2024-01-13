package io.github.kiryu1223.expressionTree.plugin;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportInfo that = (ImportInfo) o;
        return isStatic == that.isStatic && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, isStatic);
    }
}
