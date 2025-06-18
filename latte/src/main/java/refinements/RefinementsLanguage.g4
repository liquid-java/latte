grammar RefinementsLanguage;


prog: start | ;
start:
		pred	#startPred
	;


pred:
		'(' pred ')'				#predGroup
	|	'!' pred					#predNegate
	|	pred LOGOP pred 			#predLogic
	|	exp							#predExp
	;

exp:
		'(' exp ')'					#expGroup
	|	exp BOOLOP exp				#expBool
	|	operand						#expOperand
	;

operand:
		literalExpression			#opLiteral
	|	operand ARITHOP	operand		#opArith
	|	operand '-'	operand			#opSub
	|	'-' operand					#opMinus
	|	'!' operand					#opNot
	|	'(' operand ')'				#opGroup
	;


literalExpression:
		'(' literalExpression ')'	#litGroup
	|	literal						#lit
	| 	ID 							#var
	|	ID '.' ID			        #targetInvocation
	;


literal:
		BOOL
	|	INT;



LOGOP   : '&&'|'||'| '-->';
BOOLOP	 : '=='|'!='|'>='|'>'|'<='|'<';
ARITHOP : '+'|'*'|'/'|'%';//|'-';

BOOL    : 'true' | 'false';
ID_UPPER: ([A-Z][a-zA-Z0-9]*);
ID     	: '#'*[a-zA-Z_][a-zA-Z0-9_#]*;
INT     : 	(([0-9]+) |	([0-9]+('_'[0-9]+)*));

WS		:  (' '|'\t'|'\n'|'\r')+ -> channel(HIDDEN);
