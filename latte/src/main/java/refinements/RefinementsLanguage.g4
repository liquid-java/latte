grammar RefinementsLanguage;


prog: pred | ;


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
		leafs						#opLiteral
	|	operand ARITHOP	operand		#opArith
	|	operand '-'	operand			#opSub
	|	'!' operand					#opNot
	|	'(' operand ')'				#opGroup
	;


leafs:
		'(' leafs ')'				#litGroup
	|	literal						#lit
	| 	ID 							#var
	|	ID '.' ID			        #targetInvocation
	;


literal:
		BOOL	#litBool
	|	INT		#litInt
	;



LOGOP   : '&&'|'||';
BOOLOP: '==' | '<=' | '<' | '>=' | '>';
ARITHOP : '+'|'*'|'/';//|'-';

// 𝑐 | 𝑥 | 𝑥 .𝑓 | 𝑒 + 𝑒 | ! 𝑒
// | 𝑒 * 𝑒 | 𝑒 / 𝑒 | 𝑒 == 𝑒 | 𝑒 < 𝑒 | 𝑒 || 𝑒 | 𝑒 && 𝑒
// | 𝑥 (𝑥)

BOOL    : 'true' | 'false';
ID_UPPER: ([A-Z][a-zA-Z0-9]*);
ID     	: '#'*[a-zA-Z_][a-zA-Z0-9_#]*;
INT     : 	(([0-9]+) |	([0-9]+('_'[0-9]+)*));

WS		:  (' '|'\t'|'\n'|'\r')+ -> channel(HIDDEN);
