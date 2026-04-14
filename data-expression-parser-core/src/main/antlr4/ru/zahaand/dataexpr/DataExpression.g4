grammar DataExpression;

// ─── Entry ───────────────────────────────────────────────────────────────────
prog : expr EOF ;

// ─── Logical (lowest precedence) ─────────────────────────────────────────────
expr       : orExpr ;
orExpr     : andExpr   ( OR  andExpr   )* ;
andExpr    : notExpr   ( AND notExpr   )* ;
notExpr    : NOT notExpr
           | comparison
           ;

// ─── Comparison ──────────────────────────────────────────────────────────────
comparison : additive ( ( '>=' | '<=' | '>' | '<' | '==' | '!=' ) additive )?
           | additive IN     '(' valueList ')'
           | additive NOT IN '(' valueList ')'
           | additive IN     FIELD
           | additive NOT IN FIELD
           ;

valueList  : literal (',' literal)* ;

// ─── Arithmetic ──────────────────────────────────────────────────────────────
additive       : multiplicative ( ('+' | '-') multiplicative )* ;
multiplicative : power          ( ('*' | '/' | '%') power    )* ;
power          : unary          ( ('^' | STAR STAR) unary    )* ;
unary          : '-' unary
               | primary
               ;

// ─── Primary (highest precedence) ────────────────────────────────────────────
primary    : FIELD                        // [column name]
           | literal
           | ID '(' argList? ')'          // abs([x]), max([a], [b])
           | '(' expr ')'
           ;

argList    : expr (',' expr)* ;

literal    : NUMBER
           | STRING
           | TRUE
           | FALSE
           ;

// ─── Lexer ───────────────────────────────────────────────────────────────────
// Reserved words MUST appear above the ID rule
TRUE   : [Tt][Rr][Uu][Ee] ;
FALSE  : [Ff][Aa][Ll][Ss][Ee] ;
AND    : [Aa][Nn][Dd] ;
OR     : [Oo][Rr] ;
NOT    : [Nn][Oo][Tt] ;
IN     : [Ii][Nn] ;

FIELD  : '[' ~[\]\n]+ ']' ;
STRING : '\'' ~['\\]* '\'' ;
NUMBER : [0-9]+ ('.' [0-9]+)? ;
ID     : [a-zA-Z_][a-zA-Z_0-9]* ;
STAR   : '*' ;
WS     : [ \t\r\n]+ -> skip ;
