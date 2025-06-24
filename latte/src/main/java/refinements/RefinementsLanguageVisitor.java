// Generated from RefinementsLanguage.g4 by ANTLR 4.13.1
package refinements;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RefinementsLanguageParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RefinementsLanguageVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RefinementsLanguageParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(RefinementsLanguageParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredGroup(RefinementsLanguageParser.PredGroupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predNegate}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredNegate(RefinementsLanguageParser.PredNegateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predExp}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredExp(RefinementsLanguageParser.PredExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predLogic}
	 * labeled alternative in {@link RefinementsLanguageParser#pred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredLogic(RefinementsLanguageParser.PredLogicContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expBool}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpBool(RefinementsLanguageParser.ExpBoolContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expOperand}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpOperand(RefinementsLanguageParser.ExpOperandContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpGroup(RefinementsLanguageParser.ExpGroupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code opArith}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpArith(RefinementsLanguageParser.OpArithContext ctx);
	/**
	 * Visit a parse tree produced by the {@code opNot}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpNot(RefinementsLanguageParser.OpNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code opLiteral}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpLiteral(RefinementsLanguageParser.OpLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code opGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpGroup(RefinementsLanguageParser.OpGroupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code opSub}
	 * labeled alternative in {@link RefinementsLanguageParser#operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpSub(RefinementsLanguageParser.OpSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code litGroup}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLitGroup(RefinementsLanguageParser.LitGroupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lit}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLit(RefinementsLanguageParser.LitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code var}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar(RefinementsLanguageParser.VarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code targetInvocation}
	 * labeled alternative in {@link RefinementsLanguageParser#leafs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetInvocation(RefinementsLanguageParser.TargetInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefinementsLanguageParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(RefinementsLanguageParser.LiteralContext ctx);
}