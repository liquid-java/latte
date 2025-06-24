// Generated from RefinementsLanguage.g4 by ANTLR 4.13.1
package refinements;
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
		RULE_prog = 0, RULE_pred = 1, RULE_exp = 2, RULE_operand = 3, RULE_leafs = 4, 
		RULE_literal = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"prog", "pred", "exp", "operand", "leafs", "literal"
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
		public PredContext pred() {
			return getRuleContext(PredContext.class,0);
		}
		public ProgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prog; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterProg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitProg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitProg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgContext prog() throws RecognitionException {
		ProgContext _localctx = new ProgContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_prog);
		try {
			setState(14);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__2:
			case BOOL:
			case ID:
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(12);
				pred(0);
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
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterPredGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitPredGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitPredGroup(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredNegateContext extends PredContext {
		public PredContext pred() {
			return getRuleContext(PredContext.class,0);
		}
		public PredNegateContext(PredContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterPredNegate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitPredNegate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitPredNegate(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredExpContext extends PredContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public PredExpContext(PredContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterPredExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitPredExp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitPredExp(this);
			else return visitor.visitChildren(this);
		}
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
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterPredLogic(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitPredLogic(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitPredLogic(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredContext pred() throws RecognitionException {
		return pred(0);
	}

	private PredContext pred(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PredContext _localctx = new PredContext(_ctx, _parentState);
		PredContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_pred, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(24);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				_localctx = new PredGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(17);
				match(T__0);
				setState(18);
				pred(0);
				setState(19);
				match(T__1);
				}
				break;
			case 2:
				{
				_localctx = new PredNegateContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(21);
				match(T__2);
				setState(22);
				pred(3);
				}
				break;
			case 3:
				{
				_localctx = new PredExpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(23);
				exp(0);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(31);
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
					setState(26);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(27);
					match(LOGOP);
					setState(28);
					pred(3);
					}
					} 
				}
				setState(33);
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
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterExpBool(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitExpBool(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitExpBool(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpOperandContext extends ExpContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public ExpOperandContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterExpOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitExpOperand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitExpOperand(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpGroupContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ExpGroupContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterExpGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitExpGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitExpGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpContext exp() throws RecognitionException {
		return exp(0);
	}

	private ExpContext exp(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpContext _localctx = new ExpContext(_ctx, _parentState);
		ExpContext _prevctx = _localctx;
		int _startState = 4;
		enterRecursionRule(_localctx, 4, RULE_exp, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(40);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				_localctx = new ExpGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(35);
				match(T__0);
				setState(36);
				exp(0);
				setState(37);
				match(T__1);
				}
				break;
			case 2:
				{
				_localctx = new ExpOperandContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(39);
				operand(0);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(47);
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
					setState(42);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(43);
					match(BOOLOP);
					setState(44);
					exp(3);
					}
					} 
				}
				setState(49);
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
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterOpArith(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitOpArith(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitOpArith(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpNotContext extends OperandContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public OpNotContext(OperandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterOpNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitOpNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitOpNot(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpLiteralContext extends OperandContext {
		public LeafsContext leafs() {
			return getRuleContext(LeafsContext.class,0);
		}
		public OpLiteralContext(OperandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterOpLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitOpLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitOpLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OpGroupContext extends OperandContext {
		public OperandContext operand() {
			return getRuleContext(OperandContext.class,0);
		}
		public OpGroupContext(OperandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterOpGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitOpGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitOpGroup(this);
			else return visitor.visitChildren(this);
		}
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
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterOpSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitOpSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitOpSub(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperandContext operand() throws RecognitionException {
		return operand(0);
	}

	private OperandContext operand(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		OperandContext _localctx = new OperandContext(_ctx, _parentState);
		OperandContext _prevctx = _localctx;
		int _startState = 6;
		enterRecursionRule(_localctx, 6, RULE_operand, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(58);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				_localctx = new OpLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(51);
				leafs();
				}
				break;
			case 2:
				{
				_localctx = new OpNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(52);
				match(T__2);
				setState(53);
				operand(2);
				}
				break;
			case 3:
				{
				_localctx = new OpGroupContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(54);
				match(T__0);
				setState(55);
				operand(0);
				setState(56);
				match(T__1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(68);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(66);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new OpArithContext(new OperandContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_operand);
						setState(60);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(61);
						match(ARITHOP);
						setState(62);
						operand(5);
						}
						break;
					case 2:
						{
						_localctx = new OpSubContext(new OperandContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_operand);
						setState(63);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(64);
						match(T__3);
						setState(65);
						operand(4);
						}
						break;
					}
					} 
				}
				setState(70);
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
	public static class LeafsContext extends ParserRuleContext {
		public LeafsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_leafs; }
	 
		public LeafsContext() { }
		public void copyFrom(LeafsContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TargetInvocationContext extends LeafsContext {
		public List<TerminalNode> ID() { return getTokens(RefinementsLanguageParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(RefinementsLanguageParser.ID, i);
		}
		public TargetInvocationContext(LeafsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterTargetInvocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitTargetInvocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitTargetInvocation(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitGroupContext extends LeafsContext {
		public LeafsContext leafs() {
			return getRuleContext(LeafsContext.class,0);
		}
		public LitGroupContext(LeafsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterLitGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitLitGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitLitGroup(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitContext extends LeafsContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LitContext(LeafsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterLit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitLit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitLit(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class VarContext extends LeafsContext {
		public TerminalNode ID() { return getToken(RefinementsLanguageParser.ID, 0); }
		public VarContext(LeafsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterVar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitVar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LeafsContext leafs() throws RecognitionException {
		LeafsContext _localctx = new LeafsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_leafs);
		try {
			setState(80);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				_localctx = new LitGroupContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(71);
				match(T__0);
				setState(72);
				leafs();
				setState(73);
				match(T__1);
				}
				break;
			case 2:
				_localctx = new LitContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(75);
				literal();
				}
				break;
			case 3:
				_localctx = new VarContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(76);
				match(ID);
				}
				break;
			case 4:
				_localctx = new TargetInvocationContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(77);
				match(ID);
				setState(78);
				match(T__4);
				setState(79);
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
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	 
		public LiteralContext() { }
		public void copyFrom(LiteralContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitIntContext extends LiteralContext {
		public TerminalNode INT() { return getToken(RefinementsLanguageParser.INT, 0); }
		public LitIntContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterLitInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitLitInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitLitInt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LitBoolContext extends LiteralContext {
		public TerminalNode BOOL() { return getToken(RefinementsLanguageParser.BOOL, 0); }
		public LitBoolContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).enterLitBool(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefinementsLanguageListener ) ((RefinementsLanguageListener)listener).exitLitBool(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefinementsLanguageVisitor ) return ((RefinementsLanguageVisitor<? extends T>)visitor).visitLitBool(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_literal);
		try {
			setState(84);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
				_localctx = new LitBoolContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(82);
				match(BOOL);
				}
				break;
			case INT:
				_localctx = new LitIntContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(83);
				match(INT);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return pred_sempred((PredContext)_localctx, predIndex);
		case 2:
			return exp_sempred((ExpContext)_localctx, predIndex);
		case 3:
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
			return precpred(_ctx, 4);
		case 3:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\rW\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0001\u0000\u0003\u0000\u000f\b\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001\u0019\b\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0005\u0001\u001e\b\u0001\n\u0001\f\u0001!\t\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		")\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002.\b\u0002\n\u0002"+
		"\f\u00021\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003;\b\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0005"+
		"\u0003C\b\u0003\n\u0003\f\u0003F\t\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0003\u0004Q\b\u0004\u0001\u0005\u0001\u0005\u0003\u0005U\b\u0005"+
		"\u0001\u0005\u0000\u0003\u0002\u0004\u0006\u0006\u0000\u0002\u0004\u0006"+
		"\b\n\u0000\u0000^\u0000\u000e\u0001\u0000\u0000\u0000\u0002\u0018\u0001"+
		"\u0000\u0000\u0000\u0004(\u0001\u0000\u0000\u0000\u0006:\u0001\u0000\u0000"+
		"\u0000\bP\u0001\u0000\u0000\u0000\nT\u0001\u0000\u0000\u0000\f\u000f\u0003"+
		"\u0002\u0001\u0000\r\u000f\u0001\u0000\u0000\u0000\u000e\f\u0001\u0000"+
		"\u0000\u0000\u000e\r\u0001\u0000\u0000\u0000\u000f\u0001\u0001\u0000\u0000"+
		"\u0000\u0010\u0011\u0006\u0001\uffff\uffff\u0000\u0011\u0012\u0005\u0001"+
		"\u0000\u0000\u0012\u0013\u0003\u0002\u0001\u0000\u0013\u0014\u0005\u0002"+
		"\u0000\u0000\u0014\u0019\u0001\u0000\u0000\u0000\u0015\u0016\u0005\u0003"+
		"\u0000\u0000\u0016\u0019\u0003\u0002\u0001\u0003\u0017\u0019\u0003\u0004"+
		"\u0002\u0000\u0018\u0010\u0001\u0000\u0000\u0000\u0018\u0015\u0001\u0000"+
		"\u0000\u0000\u0018\u0017\u0001\u0000\u0000\u0000\u0019\u001f\u0001\u0000"+
		"\u0000\u0000\u001a\u001b\n\u0002\u0000\u0000\u001b\u001c\u0005\u0006\u0000"+
		"\u0000\u001c\u001e\u0003\u0002\u0001\u0003\u001d\u001a\u0001\u0000\u0000"+
		"\u0000\u001e!\u0001\u0000\u0000\u0000\u001f\u001d\u0001\u0000\u0000\u0000"+
		"\u001f \u0001\u0000\u0000\u0000 \u0003\u0001\u0000\u0000\u0000!\u001f"+
		"\u0001\u0000\u0000\u0000\"#\u0006\u0002\uffff\uffff\u0000#$\u0005\u0001"+
		"\u0000\u0000$%\u0003\u0004\u0002\u0000%&\u0005\u0002\u0000\u0000&)\u0001"+
		"\u0000\u0000\u0000\')\u0003\u0006\u0003\u0000(\"\u0001\u0000\u0000\u0000"+
		"(\'\u0001\u0000\u0000\u0000)/\u0001\u0000\u0000\u0000*+\n\u0002\u0000"+
		"\u0000+,\u0005\u0007\u0000\u0000,.\u0003\u0004\u0002\u0003-*\u0001\u0000"+
		"\u0000\u0000.1\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000\u0000/0\u0001"+
		"\u0000\u0000\u00000\u0005\u0001\u0000\u0000\u00001/\u0001\u0000\u0000"+
		"\u000023\u0006\u0003\uffff\uffff\u00003;\u0003\b\u0004\u000045\u0005\u0003"+
		"\u0000\u00005;\u0003\u0006\u0003\u000267\u0005\u0001\u0000\u000078\u0003"+
		"\u0006\u0003\u000089\u0005\u0002\u0000\u00009;\u0001\u0000\u0000\u0000"+
		":2\u0001\u0000\u0000\u0000:4\u0001\u0000\u0000\u0000:6\u0001\u0000\u0000"+
		"\u0000;D\u0001\u0000\u0000\u0000<=\n\u0004\u0000\u0000=>\u0005\b\u0000"+
		"\u0000>C\u0003\u0006\u0003\u0005?@\n\u0003\u0000\u0000@A\u0005\u0004\u0000"+
		"\u0000AC\u0003\u0006\u0003\u0004B<\u0001\u0000\u0000\u0000B?\u0001\u0000"+
		"\u0000\u0000CF\u0001\u0000\u0000\u0000DB\u0001\u0000\u0000\u0000DE\u0001"+
		"\u0000\u0000\u0000E\u0007\u0001\u0000\u0000\u0000FD\u0001\u0000\u0000"+
		"\u0000GH\u0005\u0001\u0000\u0000HI\u0003\b\u0004\u0000IJ\u0005\u0002\u0000"+
		"\u0000JQ\u0001\u0000\u0000\u0000KQ\u0003\n\u0005\u0000LQ\u0005\u000b\u0000"+
		"\u0000MN\u0005\u000b\u0000\u0000NO\u0005\u0005\u0000\u0000OQ\u0005\u000b"+
		"\u0000\u0000PG\u0001\u0000\u0000\u0000PK\u0001\u0000\u0000\u0000PL\u0001"+
		"\u0000\u0000\u0000PM\u0001\u0000\u0000\u0000Q\t\u0001\u0000\u0000\u0000"+
		"RU\u0005\t\u0000\u0000SU\u0005\f\u0000\u0000TR\u0001\u0000\u0000\u0000"+
		"TS\u0001\u0000\u0000\u0000U\u000b\u0001\u0000\u0000\u0000\n\u000e\u0018"+
		"\u001f(/:BDPT";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}