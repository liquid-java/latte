// Generated from RefinementsLanguage.g4 by ANTLR 4.13.1
package refinements;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RefinementsLanguageParser}.
 */
public interface RefinementsLanguageListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RefinementsLanguageParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(RefinementsLanguageParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefinementsLanguageParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(RefinementsLanguageParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void enterPredGroup(RefinementsLanguageParser.PredGroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void exitPredGroup(RefinementsLanguageParser.PredGroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predNegate}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void enterPredNegate(RefinementsLanguageParser.PredNegateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predNegate}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void exitPredNegate(RefinementsLanguageParser.PredNegateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predExp}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void enterPredExp(RefinementsLanguageParser.PredExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predExp}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void exitPredExp(RefinementsLanguageParser.PredExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predImplies}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void enterPredImplies(RefinementsLanguageParser.PredImpliesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predImplies}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void exitPredImplies(RefinementsLanguageParser.PredImpliesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predLogic}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void enterPredLogic(RefinementsLanguageParser.PredLogicContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predLogic}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 */
	void exitPredLogic(RefinementsLanguageParser.PredLogicContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expBool}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExpBool(RefinementsLanguageParser.ExpBoolContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expBool}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExpBool(RefinementsLanguageParser.ExpBoolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expOperand}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExpOperand(RefinementsLanguageParser.ExpOperandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expOperand}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExpOperand(RefinementsLanguageParser.ExpOperandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExpGroup(RefinementsLanguageParser.ExpGroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExpGroup(RefinementsLanguageParser.ExpGroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code opArith}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOpArith(RefinementsLanguageParser.OpArithContext ctx);
	/**
	 * Exit a parse tree produced by the {@code opArith}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOpArith(RefinementsLanguageParser.OpArithContext ctx);
	/**
	 * Enter a parse tree produced by the {@code opNot}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOpNot(RefinementsLanguageParser.OpNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code opNot}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOpNot(RefinementsLanguageParser.OpNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code opLiteral}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOpLiteral(RefinementsLanguageParser.OpLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code opLiteral}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOpLiteral(RefinementsLanguageParser.OpLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code opGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOpGroup(RefinementsLanguageParser.OpGroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code opGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOpGroup(RefinementsLanguageParser.OpGroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code opSub}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOpSub(RefinementsLanguageParser.OpSubContext ctx);
	/**
	 * Exit a parse tree produced by the {@code opSub}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOpSub(RefinementsLanguageParser.OpSubContext ctx);
	/**
	 * Enter a parse tree produced by the {@code litGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void enterLitGroup(RefinementsLanguageParser.LitGroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code litGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void exitLitGroup(RefinementsLanguageParser.LitGroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lit}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void enterLit(RefinementsLanguageParser.LitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lit}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void exitLit(RefinementsLanguageParser.LitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code var}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void enterVar(RefinementsLanguageParser.VarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code var}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void exitVar(RefinementsLanguageParser.VarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code targetInvocation}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void enterTargetInvocation(RefinementsLanguageParser.TargetInvocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code targetInvocation}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 */
	void exitTargetInvocation(RefinementsLanguageParser.TargetInvocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code litBool}
	 * labeled alternative in {@link RefinementsLanguageParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLitBool(RefinementsLanguageParser.LitBoolContext ctx);
	/**
	 * Exit a parse tree produced by the {@code litBool}
	 * labeled alternative in {@link RefinementsLanguageParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLitBool(RefinementsLanguageParser.LitBoolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code litInt}
	 * labeled alternative in {@link RefinementsLanguageParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLitInt(RefinementsLanguageParser.LitIntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code litInt}
	 * labeled alternative in {@link RefinementsLanguageParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLitInt(RefinementsLanguageParser.LitIntContext ctx);
}