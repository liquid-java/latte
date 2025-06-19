// Generated from /Users/cgamboa/git/latte/latte/src/main/java/refinements/RefinementsLanguage.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class RefinementsLanguageParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, LOGOP=6, BOOLOP=7, ARITHOP=8, 
		BOOL=9, ID_UPPER=10, ID=11, INT=12, WS=13;
	public static final int
		RULE_prog = 0, RULE_start = 1, RULE_pred = 2, RULE_exp = 3, RULE_operand = 4, 
		RULE_literalExpression = 5, RULE_literal = 6;
	private static String[] makeRuleNames() {
		return new String[] {
			"prog", "start", "pred", "exp", "operand", "literalExpression", "literal"
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

	@Override
	public String getGrammarFileName() { return "RefinementsLanguage.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RefinementsLanguageParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgContext extends ParserRuleContext {
		public StartContext start() {
			return getRuleContext(StartContext.class,0);
		}
		public ProgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prog; }
	}

	public final ProgContext prog() throws RecognitionException {
		ProgContext _localctx = new ProgContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_prog);
		try {
			setState(16);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__2:
			case T__3:
			case BOOL:
			case ID:
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(14);
				start();
				}
				break;
			case EOF:
				enterOuterAlt(_localctx, 2);
				{
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartContext extends ParserRuleContext {
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
	 
		public StartContext() { }
		public void copyFrom(StartContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StartPredContext extends StartContext {
		public PredContext pred() {
			return getRuleContext(PredContext.class,0);
		}
		public StartPredContext(StartContext ctx) { copyFrom(ctx); }
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_start);
		try {
			_localctx = new StartPredContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(18);
			pred(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PredContext extends ParserRuleContext {
		public PredContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pred; }
	 
		public PredContext() { }
		public void copyFrom(PredContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredGroupContext extends PredContext {
		public PredContext pred() {
			return getRuleContext(PredContext.class,0);
		}
		public PredGroupContext(PredContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredNegateContext extends PredContext {
		public PredContext pred() {
			return getRuleContext(PredContext.class,0);
		}
		public PredNegateContext(PredContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredExpContext extends PredContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public PredExpContext(PredContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredLogicContext extends PredContext {
		public List<PredContext> pred() {
			return getRuleContexts(PredContext.class);
		}
		public PredContext pred(int i) {
			return getRuleContext(PredContext.class,i);
		}
		public TerminalNode LOGOP() { return getToken(RefinementsLanguageParser.LOGOP, 0); }
		public PredLogicContext(PredContext ctx) { copyFrom(ctx); }
	}

	public final PredContext pred() throws RecognitionException {
		return pred(0);
	}

	private PredContext pred(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PredContext _localctx = new PredContext(_ctx, _parentState);
		PredContext _prevctx = _localctx;
		int _startState = 4;
		enterRecursionRule(_localctx, 4, RULE_pred, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(28);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				_localctx = new PredGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(21);
				match(T__0);
				setState(22);
				pred(0);
				setState(23);
				match(T__1);
				}
				break;
			case 2:
				{
				_localctx = new PredNegateContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(25);
				match(T__2);
				setState(26);
				pred(3);
				}
				break;
			case 3:
				{
				_localctx = new PredExpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(27);
				exp(0);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(35);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new PredLogicContext(new PredContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_pred);
					setState(30);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(31);
					match(LOGOP);
					setState(32);
					pred(3);
					}
					} 
				}
				setState(37);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpContext extends ParserRuleContext {
		public ExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exp; }
	 
		public ExpContext() { }
		public void copyFrom(ExpContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpBoolContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public TerminalNode BOOLOP() { return getToken(RefinementsLanguageParser.BOOLOP, 0); }
		public ExpBoolContext(ExpContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpOperandContext extends ExpContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public ExpOperandContext(ExpContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpGroupContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ExpGroupContext(ExpContext ctx) { copyFrom(ctx); }
	}

	public final ExpContext exp() throws RecognitionException {
		return exp(0);
	}

	private ExpContext exp(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpContext _localctx = new ExpContext(_ctx, _parentState);
		ExpContext _prevctx = _localctx;
		int _startState = 6;
		enterRecursionRule(_localctx, 6, RULE_exp, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(44);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				_localctx = new ExpGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(39);
				match(T__0);
				setState(40);
				exp(0);
				setState(41);
				match(T__1);
				}
				break;
			case 2:
				{
				_localctx = new ExpOperandContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(43);
				operand(0);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(51);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ExpBoolContext(new ExpContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_exp);
					setState(46);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(47);
					match(BOOLOP);
					setState(48);
					exp(3);
					}
					} 
				}
				setState(53);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OperandContext extends ParserRuleContext {
		public OperandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operand; }
	 
		public OperandContext() { }
		public void copyFrom(OperandContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpArithContext extends OperandContext {
		public List<OperandContext> operand() {
			return getRuleContexts(OperandContext.class);
		}
		public OperandContext operand(int i) {
			return getRuleContext(OperandContext.class,i);
		}
		public TerminalNode ARITHOP() { return getToken(RefinementsLanguageParser.ARITHOP, 0); }
		public OpArithContext(OperandContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpNotContext extends OperandContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public OpNotContext(OperandContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpLiteralContext extends OperandContext {
		public LiteralExpressionContext literalExpression() {
			return getRuleContext(LiteralExpressionContext.class,0);
		}
		public OpLiteralContext(OperandContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpGroupContext extends OperandContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public OpGroupContext(OperandContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpMinusContext extends OperandContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public OpMinusContext(OperandContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpSubContext extends OperandContext {
		public List<OperandContext> operand() {
			return getRuleContexts(OperandContext.class);
		}
		public OperandContext operand(int i) {
			return getRuleContext(OperandContext.class,i);
		}
		public OpSubContext(OperandContext ctx) { copyFrom(ctx); }
	}

	public final OperandContext operand() throws RecognitionException {
		return operand(0);
	}

	private OperandContext operand(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		OperandContext _localctx = new OperandContext(_ctx, _parentState);
		OperandContext _prevctx = _localctx;
		int _startState = 8;
		enterRecursionRule(_localctx, 8, RULE_operand, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				_localctx = new OpLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(55);
				literalExpression();
				}
				break;
			case 2:
				{
				_localctx = new OpMinusContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(56);
				match(T__3);
				setState(57);
				operand(3);
				}
				break;
			case 3:
				{
				_localctx = new OpNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(58);
				match(T__2);
				setState(59);
				operand(2);
				}
				break;
			case 4:
				{
				_localctx = new OpGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(60);
				match(T__0);
				setState(61);
				operand(0);
				setState(62);
				match(T__1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(74);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(72);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new OpArithContext(new OperandContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_operand);
						setState(66);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(67);
						match(ARITHOP);
						setState(68);
						operand(6);
						}
						break;
					case 2:
						{
						_localctx = new OpSubContext(new OperandContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_operand);
						setState(69);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(70);
						match(T__3);
						setState(71);
						operand(5);
						}
						break;
					}
					} 
				}
				setState(76);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralExpressionContext extends ParserRuleContext {
		public LiteralExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literalExpression; }
	 
		public LiteralExpressionContext() { }
		public void copyFrom(LiteralExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TargetInvocationContext extends LiteralExpressionContext {
		public List<TerminalNode> ID() { return getTokens(RefinementsLanguageParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(RefinementsLanguageParser.ID, i);
		}
		public TargetInvocationContext(LiteralExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitGroupContext extends LiteralExpressionContext {
		public LiteralExpressionContext literalExpression() {
			return getRuleContext(LiteralExpressionContext.class,0);
		}
		public LitGroupContext(LiteralExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitContext extends LiteralExpressionContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LitContext(LiteralExpressionContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class VarContext extends LiteralExpressionContext {
		public TerminalNode ID() { return getToken(RefinementsLanguageParser.ID, 0); }
		public VarContext(LiteralExpressionContext ctx) { copyFrom(ctx); }
	}

	public final LiteralExpressionContext literalExpression() throws RecognitionException {
		LiteralExpressionContext _localctx = new LiteralExpressionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_literalExpression);
		try {
			setState(86);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				_localctx = new LitGroupContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(77);
				match(T__0);
				setState(78);
				literalExpression();
				setState(79);
				match(T__1);
				}
				break;
			case 2:
				_localctx = new LitContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(81);
				literal();
				}
				break;
			case 3:
				_localctx = new VarContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(82);
				match(ID);
				}
				break;
			case 4:
				_localctx = new TargetInvocationContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(83);
				match(ID);
				setState(84);
				match(T__4);
				setState(85);
				match(ID);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(RefinementsLanguageParser.BOOL, 0); }
		public TerminalNode INT() { return getToken(RefinementsLanguageParser.INT, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			_la = _input.LA(1);
			if ( !(_la==BOOL || _la==INT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 2:
			return pred_sempred((PredContext)_localctx, predIndex);
		case 3:
			return exp_sempred((ExpContext)_localctx, predIndex);
		case 4:
			return operand_sempred((OperandContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean pred_sempred(PredContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean exp_sempred(ExpContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean operand_sempred(OperandContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 5);
		case 3:
			return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\r[\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0001\u0000\u0001\u0000\u0003"+
		"\u0000\u0011\b\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u0002\u001d\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\""+
		"\b\u0002\n\u0002\f\u0002%\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003-\b\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0005\u00032\b\u0003\n\u0003\f\u00035\t\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004A\b\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0005\u0004I\b\u0004\n\u0004\f\u0004L\t\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005W\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0000\u0003\u0004\u0006\b\u0007\u0000\u0002\u0004\u0006\b\n\f\u0000\u0001"+
		"\u0002\u0000\t\t\f\fa\u0000\u0010\u0001\u0000\u0000\u0000\u0002\u0012"+
		"\u0001\u0000\u0000\u0000\u0004\u001c\u0001\u0000\u0000\u0000\u0006,\u0001"+
		"\u0000\u0000\u0000\b@\u0001\u0000\u0000\u0000\nV\u0001\u0000\u0000\u0000"+
		"\fX\u0001\u0000\u0000\u0000\u000e\u0011\u0003\u0002\u0001\u0000\u000f"+
		"\u0011\u0001\u0000\u0000\u0000\u0010\u000e\u0001\u0000\u0000\u0000\u0010"+
		"\u000f\u0001\u0000\u0000\u0000\u0011\u0001\u0001\u0000\u0000\u0000\u0012"+
		"\u0013\u0003\u0004\u0002\u0000\u0013\u0003\u0001\u0000\u0000\u0000\u0014"+
		"\u0015\u0006\u0002\uffff\uffff\u0000\u0015\u0016\u0005\u0001\u0000\u0000"+
		"\u0016\u0017\u0003\u0004\u0002\u0000\u0017\u0018\u0005\u0002\u0000\u0000"+
		"\u0018\u001d\u0001\u0000\u0000\u0000\u0019\u001a\u0005\u0003\u0000\u0000"+
		"\u001a\u001d\u0003\u0004\u0002\u0003\u001b\u001d\u0003\u0006\u0003\u0000"+
		"\u001c\u0014\u0001\u0000\u0000\u0000\u001c\u0019\u0001\u0000\u0000\u0000"+
		"\u001c\u001b\u0001\u0000\u0000\u0000\u001d#\u0001\u0000\u0000\u0000\u001e"+
		"\u001f\n\u0002\u0000\u0000\u001f \u0005\u0006\u0000\u0000 \"\u0003\u0004"+
		"\u0002\u0003!\u001e\u0001\u0000\u0000\u0000\"%\u0001\u0000\u0000\u0000"+
		"#!\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000\u0000$\u0005\u0001\u0000"+
		"\u0000\u0000%#\u0001\u0000\u0000\u0000&\'\u0006\u0003\uffff\uffff\u0000"+
		"\'(\u0005\u0001\u0000\u0000()\u0003\u0006\u0003\u0000)*\u0005\u0002\u0000"+
		"\u0000*-\u0001\u0000\u0000\u0000+-\u0003\b\u0004\u0000,&\u0001\u0000\u0000"+
		"\u0000,+\u0001\u0000\u0000\u0000-3\u0001\u0000\u0000\u0000./\n\u0002\u0000"+
		"\u0000/0\u0005\u0007\u0000\u000002\u0003\u0006\u0003\u00031.\u0001\u0000"+
		"\u0000\u000025\u0001\u0000\u0000\u000031\u0001\u0000\u0000\u000034\u0001"+
		"\u0000\u0000\u00004\u0007\u0001\u0000\u0000\u000053\u0001\u0000\u0000"+
		"\u000067\u0006\u0004\uffff\uffff\u00007A\u0003\n\u0005\u000089\u0005\u0004"+
		"\u0000\u00009A\u0003\b\u0004\u0003:;\u0005\u0003\u0000\u0000;A\u0003\b"+
		"\u0004\u0002<=\u0005\u0001\u0000\u0000=>\u0003\b\u0004\u0000>?\u0005\u0002"+
		"\u0000\u0000?A\u0001\u0000\u0000\u0000@6\u0001\u0000\u0000\u0000@8\u0001"+
		"\u0000\u0000\u0000@:\u0001\u0000\u0000\u0000@<\u0001\u0000\u0000\u0000"+
		"AJ\u0001\u0000\u0000\u0000BC\n\u0005\u0000\u0000CD\u0005\b\u0000\u0000"+
		"DI\u0003\b\u0004\u0006EF\n\u0004\u0000\u0000FG\u0005\u0004\u0000\u0000"+
		"GI\u0003\b\u0004\u0005HB\u0001\u0000\u0000\u0000HE\u0001\u0000\u0000\u0000"+
		"IL\u0001\u0000\u0000\u0000JH\u0001\u0000\u0000\u0000JK\u0001\u0000\u0000"+
		"\u0000K\t\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000MN\u0005\u0001"+
		"\u0000\u0000NO\u0003\n\u0005\u0000OP\u0005\u0002\u0000\u0000PW\u0001\u0000"+
		"\u0000\u0000QW\u0003\f\u0006\u0000RW\u0005\u000b\u0000\u0000ST\u0005\u000b"+
		"\u0000\u0000TU\u0005\u0005\u0000\u0000UW\u0005\u000b\u0000\u0000VM\u0001"+
		"\u0000\u0000\u0000VQ\u0001\u0000\u0000\u0000VR\u0001\u0000\u0000\u0000"+
		"VS\u0001\u0000\u0000\u0000W\u000b\u0001\u0000\u0000\u0000XY\u0007\u0000"+
		"\u0000\u0000Y\r\u0001\u0000\u0000\u0000\t\u0010\u001c#,3@HJV";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}