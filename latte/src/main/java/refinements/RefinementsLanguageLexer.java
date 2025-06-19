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
		"\u0004\u0000\ry\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005-\b\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0003\u00069\b\u0006\u0001\u0007\u0001\u0007\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003"+
		"\bF\b\b\u0001\t\u0001\t\u0005\tJ\b\t\n\t\f\tM\t\t\u0001\n\u0005\nP\b\n"+
		"\n\n\f\nS\t\n\u0001\n\u0001\n\u0005\nW\b\n\n\n\f\nZ\t\n\u0001\u000b\u0004"+
		"\u000b]\b\u000b\u000b\u000b\f\u000b^\u0001\u000b\u0004\u000bb\b\u000b"+
		"\u000b\u000b\f\u000bc\u0001\u000b\u0001\u000b\u0004\u000bh\b\u000b\u000b"+
		"\u000b\f\u000bi\u0005\u000bl\b\u000b\n\u000b\f\u000bo\t\u000b\u0003\u000b"+
		"q\b\u000b\u0001\f\u0004\ft\b\f\u000b\f\f\fu\u0001\f\u0001\f\u0000\u0000"+
		"\r\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006"+
		"\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u0001\u0000"+
		"\u0007\u0003\u0000%%*+//\u0001\u0000AZ\u0003\u000009AZaz\u0003\u0000A"+
		"Z__az\u0005\u0000##09AZ__az\u0001\u000009\u0003\u0000\t\n\r\r  \u0089"+
		"\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000"+
		"\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000"+
		"\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000"+
		"\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011"+
		"\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015"+
		"\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019"+
		"\u0001\u0000\u0000\u0000\u0001\u001b\u0001\u0000\u0000\u0000\u0003\u001d"+
		"\u0001\u0000\u0000\u0000\u0005\u001f\u0001\u0000\u0000\u0000\u0007!\u0001"+
		"\u0000\u0000\u0000\t#\u0001\u0000\u0000\u0000\u000b,\u0001\u0000\u0000"+
		"\u0000\r8\u0001\u0000\u0000\u0000\u000f:\u0001\u0000\u0000\u0000\u0011"+
		"E\u0001\u0000\u0000\u0000\u0013G\u0001\u0000\u0000\u0000\u0015Q\u0001"+
		"\u0000\u0000\u0000\u0017p\u0001\u0000\u0000\u0000\u0019s\u0001\u0000\u0000"+
		"\u0000\u001b\u001c\u0005(\u0000\u0000\u001c\u0002\u0001\u0000\u0000\u0000"+
		"\u001d\u001e\u0005)\u0000\u0000\u001e\u0004\u0001\u0000\u0000\u0000\u001f"+
		" \u0005!\u0000\u0000 \u0006\u0001\u0000\u0000\u0000!\"\u0005-\u0000\u0000"+
		"\"\b\u0001\u0000\u0000\u0000#$\u0005.\u0000\u0000$\n\u0001\u0000\u0000"+
		"\u0000%&\u0005&\u0000\u0000&-\u0005&\u0000\u0000\'(\u0005|\u0000\u0000"+
		"(-\u0005|\u0000\u0000)*\u0005-\u0000\u0000*+\u0005-\u0000\u0000+-\u0005"+
		">\u0000\u0000,%\u0001\u0000\u0000\u0000,\'\u0001\u0000\u0000\u0000,)\u0001"+
		"\u0000\u0000\u0000-\f\u0001\u0000\u0000\u0000./\u0005=\u0000\u0000/9\u0005"+
		"=\u0000\u000001\u0005!\u0000\u000019\u0005=\u0000\u000023\u0005>\u0000"+
		"\u000039\u0005=\u0000\u000049\u0005>\u0000\u000056\u0005<\u0000\u0000"+
		"69\u0005=\u0000\u000079\u0005<\u0000\u00008.\u0001\u0000\u0000\u00008"+
		"0\u0001\u0000\u0000\u000082\u0001\u0000\u0000\u000084\u0001\u0000\u0000"+
		"\u000085\u0001\u0000\u0000\u000087\u0001\u0000\u0000\u00009\u000e\u0001"+
		"\u0000\u0000\u0000:;\u0007\u0000\u0000\u0000;\u0010\u0001\u0000\u0000"+
		"\u0000<=\u0005t\u0000\u0000=>\u0005r\u0000\u0000>?\u0005u\u0000\u0000"+
		"?F\u0005e\u0000\u0000@A\u0005f\u0000\u0000AB\u0005a\u0000\u0000BC\u0005"+
		"l\u0000\u0000CD\u0005s\u0000\u0000DF\u0005e\u0000\u0000E<\u0001\u0000"+
		"\u0000\u0000E@\u0001\u0000\u0000\u0000F\u0012\u0001\u0000\u0000\u0000"+
		"GK\u0007\u0001\u0000\u0000HJ\u0007\u0002\u0000\u0000IH\u0001\u0000\u0000"+
		"\u0000JM\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000\u0000KL\u0001\u0000"+
		"\u0000\u0000L\u0014\u0001\u0000\u0000\u0000MK\u0001\u0000\u0000\u0000"+
		"NP\u0005#\u0000\u0000ON\u0001\u0000\u0000\u0000PS\u0001\u0000\u0000\u0000"+
		"QO\u0001\u0000\u0000\u0000QR\u0001\u0000\u0000\u0000RT\u0001\u0000\u0000"+
		"\u0000SQ\u0001\u0000\u0000\u0000TX\u0007\u0003\u0000\u0000UW\u0007\u0004"+
		"\u0000\u0000VU\u0001\u0000\u0000\u0000WZ\u0001\u0000\u0000\u0000XV\u0001"+
		"\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000Y\u0016\u0001\u0000\u0000"+
		"\u0000ZX\u0001\u0000\u0000\u0000[]\u0007\u0005\u0000\u0000\\[\u0001\u0000"+
		"\u0000\u0000]^\u0001\u0000\u0000\u0000^\\\u0001\u0000\u0000\u0000^_\u0001"+
		"\u0000\u0000\u0000_q\u0001\u0000\u0000\u0000`b\u0007\u0005\u0000\u0000"+
		"a`\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ca\u0001\u0000\u0000"+
		"\u0000cd\u0001\u0000\u0000\u0000dm\u0001\u0000\u0000\u0000eg\u0005_\u0000"+
		"\u0000fh\u0007\u0005\u0000\u0000gf\u0001\u0000\u0000\u0000hi\u0001\u0000"+
		"\u0000\u0000ig\u0001\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jl\u0001"+
		"\u0000\u0000\u0000ke\u0001\u0000\u0000\u0000lo\u0001\u0000\u0000\u0000"+
		"mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000nq\u0001\u0000\u0000"+
		"\u0000om\u0001\u0000\u0000\u0000p\\\u0001\u0000\u0000\u0000pa\u0001\u0000"+
		"\u0000\u0000q\u0018\u0001\u0000\u0000\u0000rt\u0007\u0006\u0000\u0000"+
		"sr\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000\u0000us\u0001\u0000\u0000"+
		"\u0000uv\u0001\u0000\u0000\u0000vw\u0001\u0000\u0000\u0000wx\u0006\f\u0000"+
		"\u0000x\u001a\u0001\u0000\u0000\u0000\r\u0000,8EKQX^cimpu\u0001\u0000"+
		"\u0001\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}