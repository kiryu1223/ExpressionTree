package io.github.kiryu1223.expressionTree.expressions;

public enum OperatorType
{
    /**
     * Unary operators, of type Unary.
     */
    POS("+"),                             // +
    NEG("-"),                             // -
    NOT("!"),                             // !
    COMPL("~"),                           // ~
    PREINC("++"),                         // ++ _
    PREDEC("--"),                         // -- _
    POSTINC("++"),                        // _ ++
    POSTDEC("--"),                        // _ --

    /**
     * unary operator for null reference checks, only used internally.
     */
    NULLCHK(""),

    /**
     * Binary operators, of type Binary.
     */
    OR("||"),                             // ||
    AND("&&"),                            // &&
    BITOR("|"),                           // |
    BITXOR("^"),                          // ^
    BITAND("&"),                          // &
    EQ("=="),                             // ==
    NE("!="),                             // !=
    LT("<"),                              // <
    GT(">"),                              // >
    LE("<="),                             // <=
    GE(">="),                             // >=
    SL("<<"),                             // <<
    SR(">>"),                             // >>
    USR(">>>"),                           // >>>
    PLUS("+"),                            // +
    MINUS("-"),                           // -
    MUL("*"),                             // *
    DIV("/"),                             // /
    MOD("%"),                             // %

    /**
     * Assignment operators, of type Assignop.
     */
    BITOR_ASG("|="),                      // |=
    BITXOR_ASG("^="),                     // ^=
    BITAND_ASG("&="),                     // &=

    SL_ASG("<<="),                        // <<=
    SR_ASG(">>="),                        // >>=
    USR_ASG(">>>="),                      // >>>=
    PLUS_ASG("+="),                       // +=
    MINUS_ASG("-="),                      // -=
    MUL_ASG("*="),                        // *=
    DIV_ASG("/="),                        // /=
    MOD_ASG("%="),                        // %=

    /**
     * ----------------------------------------
     */
    POW(""),
    CONTAINS(""),
    STARTSWITH(""),
    ENDSWITH(""),
    ;

    private final String operator;

    OperatorType(String operator)
    {
        this.operator = operator;
    }

    public String getOperator()
    {
        return operator;
    }
}
