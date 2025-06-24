// Generated from RefinementsLanguage.g4 by ANTLR 4.13.1
package refinements;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class RefinementsLanguageLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, LOGOP=6, BOOLOP=7, ARITHOP=8, 
		BOOL=9, ID_UPPER=10, ID=11, INT=12, WS=13;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "LOGOP", "BOOLOP", "ARITHOP", 
			"BOOL", "ID_UPPER", "ID", "INT", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'!'", "'-'", "'.'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, "LOGOP", "BOOLOP", "ARITHOP", "BOOL", 
			"ID_UPPER", "ID", "INT", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public RefinementsLanguageLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "RefinementsLanguage.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\rq\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005*\b"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003"+
		"\u00061\b\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b>\b\b\u0001\t\u0001"+
		"\t\u0005\tB\b\t\n\t\f\tE\t\t\u0001\n\u0005\nH\b\n\n\n\f\nK\t\n\u0001\n"+
		"\u0001\n\u0005\nO\b\n\n\n\f\nR\t\n\u0001\u000b\u0004\u000bU\b\u000b\u000b"+
		"\u000b\f\u000bV\u0001\u000b\u0004\u000bZ\b\u000b\u000b\u000b\f\u000b["+
		"\u0001\u000b\u0001\u000b\u0004\u000b`\b\u000b\u000b\u000b\f\u000ba\u0005"+
		"\u000bd\b\u000b\n\u000b\f\u000bg\t\u000b\u0003\u000bi\b\u000b\u0001\f"+
		"\u0004\fl\b\f\u000b\f\f\fm\u0001\f\u0001\f\u0000\u0000\r\u0001\u0001\u0003"+
		"\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011"+
		"\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u0001\u0000\u0007\u0002\u0000*"+
		"+//\u0001\u0000AZ\u0003\u000009AZaz\u0003\u0000AZ__az\u0005\u0000##09"+
		"AZ__az\u0001\u000009\u0003\u0000\t\n\r\r  }\u0000\u0001\u0001\u0000\u0000"+
		"\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000"+
		"\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000"+
		"\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000"+
		"\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000"+
		"\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000"+
		"\u0017\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0001"+
		"\u001b\u0001\u0000\u0000\u0000\u0003\u001d\u0001\u0000\u0000\u0000\u0005"+
		"\u001f\u0001\u0000\u0000\u0000\u0007!\u0001\u0000\u0000\u0000\t#\u0001"+
		"\u0000\u0000\u0000\u000b)\u0001\u0000\u0000\u0000\r0\u0001\u0000\u0000"+
		"\u0000\u000f2\u0001\u0000\u0000\u0000\u0011=\u0001\u0000\u0000\u0000\u0013"+
		"?\u0001\u0000\u0000\u0000\u0015I\u0001\u0000\u0000\u0000\u0017h\u0001"+
		"\u0000\u0000\u0000\u0019k\u0001\u0000\u0000\u0000\u001b\u001c\u0005(\u0000"+
		"\u0000\u001c\u0002\u0001\u0000\u0000\u0000\u001d\u001e\u0005)\u0000\u0000"+
		"\u001e\u0004\u0001\u0000\u0000\u0000\u001f \u0005!\u0000\u0000 \u0006"+
		"\u0001\u0000\u0000\u0000!\"\u0005-\u0000\u0000\"\b\u0001\u0000\u0000\u0000"+
		"#$\u0005.\u0000\u0000$\n\u0001\u0000\u0000\u0000%&\u0005&\u0000\u0000"+
		"&*\u0005&\u0000\u0000\'(\u0005|\u0000\u0000(*\u0005|\u0000\u0000)%\u0001"+
		"\u0000\u0000\u0000)\'\u0001\u0000\u0000\u0000*\f\u0001\u0000\u0000\u0000"+
		"+,\u0005=\u0000\u0000,1\u0005=\u0000\u0000-.\u0005<\u0000\u0000.1\u0005"+
		"=\u0000\u0000/1\u0005<\u0000\u00000+\u0001\u0000\u0000\u00000-\u0001\u0000"+
		"\u0000\u00000/\u0001\u0000\u0000\u00001\u000e\u0001\u0000\u0000\u0000"+
		"23\u0007\u0000\u0000\u00003\u0010\u0001\u0000\u0000\u000045\u0005t\u0000"+
		"\u000056\u0005r\u0000\u000067\u0005u\u0000\u00007>\u0005e\u0000\u0000"+
		"89\u0005f\u0000\u00009:\u0005a\u0000\u0000:;\u0005l\u0000\u0000;<\u0005"+
		"s\u0000\u0000<>\u0005e\u0000\u0000=4\u0001\u0000\u0000\u0000=8\u0001\u0000"+
		"\u0000\u0000>\u0012\u0001\u0000\u0000\u0000?C\u0007\u0001\u0000\u0000"+
		"@B\u0007\u0002\u0000\u0000A@\u0001\u0000\u0000\u0000BE\u0001\u0000\u0000"+
		"\u0000CA\u0001\u0000\u0000\u0000CD\u0001\u0000\u0000\u0000D\u0014\u0001"+
		"\u0000\u0000\u0000EC\u0001\u0000\u0000\u0000FH\u0005#\u0000\u0000GF\u0001"+
		"\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000"+
		"IJ\u0001\u0000\u0000\u0000JL\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000"+
		"\u0000LP\u0007\u0003\u0000\u0000MO\u0007\u0004\u0000\u0000NM\u0001\u0000"+
		"\u0000\u0000OR\u0001\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000PQ\u0001"+
		"\u0000\u0000\u0000Q\u0016\u0001\u0000\u0000\u0000RP\u0001\u0000\u0000"+
		"\u0000SU\u0007\u0005\u0000\u0000TS\u0001\u0000\u0000\u0000UV\u0001\u0000"+
		"\u0000\u0000VT\u0001\u0000\u0000\u0000VW\u0001\u0000\u0000\u0000Wi\u0001"+
		"\u0000\u0000\u0000XZ\u0007\u0005\u0000\u0000YX\u0001\u0000\u0000\u0000"+
		"Z[\u0001\u0000\u0000\u0000[Y\u0001\u0000\u0000\u0000[\\\u0001\u0000\u0000"+
		"\u0000\\e\u0001\u0000\u0000\u0000]_\u0005_\u0000\u0000^`\u0007\u0005\u0000"+
		"\u0000_^\u0001\u0000\u0000\u0000`a\u0001\u0000\u0000\u0000a_\u0001\u0000"+
		"\u0000\u0000ab\u0001\u0000\u0000\u0000bd\u0001\u0000\u0000\u0000c]\u0001"+
		"\u0000\u0000\u0000dg\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000\u0000"+
		"ef\u0001\u0000\u0000\u0000fi\u0001\u0000\u0000\u0000ge\u0001\u0000\u0000"+
		"\u0000hT\u0001\u0000\u0000\u0000hY\u0001\u0000\u0000\u0000i\u0018\u0001"+
		"\u0000\u0000\u0000jl\u0007\u0006\u0000\u0000kj\u0001\u0000\u0000\u0000"+
		"lm\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000"+
		"\u0000no\u0001\u0000\u0000\u0000op\u0006\f\u0000\u0000p\u001a\u0001\u0000"+
		"\u0000\u0000\r\u0000)0=CIPV[aehm\u0001\u0000\u0001\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}