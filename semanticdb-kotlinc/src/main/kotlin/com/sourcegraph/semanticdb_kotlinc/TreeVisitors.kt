package com.sourcegraph.semanticdb_kotlinc

import com.sourcegraph.semanticdb_kotlinc.Semanticdb.SymbolOccurrence.Role
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class TextDocumentBuildingVisitor(
    sourceroot: Path,
    resolver: DescriptorResolver,
    private val file: KtFile,
    lineMap: LineMap,
    globals: GlobalSymbolsCache,
    locals: LocalSymbolsCache = LocalSymbolsCache()
): SymbolGenVisitor(resolver, globals, locals) {
    private val emitter = SemanticdbTextDocumentEmitter(sourceroot, file, lineMap)

    fun build(): Semanticdb.TextDocument {
        super.visitKtFile(file, Unit)
        return emitter.buildSemanticdbTextDocument()
    }

    override fun visitClass(klass: KtClass, u: Unit?): Symbol? {
        val symbol = super.visitClass(klass, Unit)
        emitter.emitSemanticdbData(symbol!!, klass, Role.DEFINITION)
        return null
    }

    override fun visitNamedFunction(function: KtNamedFunction, u: Unit?): Symbol? {
        val symbol = super.visitNamedFunction(function, Unit)
        emitter.emitSemanticdbData(symbol!!, function, Role.DEFINITION)
        return null
    }

    override fun visitProperty(property: KtProperty, u: Unit?): Symbol? {
        val symbol = super.visitProperty(property, Unit)
        emitter.emitSemanticdbData(symbol!!, property, Role.DEFINITION)
        return null
    }

    override fun visitCallExpression(expression: KtCallExpression, u: Unit?): Symbol? {
        val symbol = super.visitCallExpression(expression, Unit)
        emitter.emitSemanticdbData(symbol!!, expression, Role.REFERENCE)
        return null
    }

    override fun visitParameter(parameter: KtParameter, u: Unit?): Symbol? {
        val symbol = super.visitParameter(parameter, Unit)
        emitter.emitSemanticdbData(symbol!!, parameter, Role.DEFINITION)
        return null
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, u: Unit?): Symbol? {
        val symbol = super.visitTypeParameter(parameter, Unit)
        emitter.emitSemanticdbData(symbol!!, parameter, Role.DEFINITION)
        return null
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, u: Unit?): Symbol? {
        val symbol = super.visitTypeAlias(typeAlias, Unit)
        emitter.emitSemanticdbData(symbol!!, typeAlias, Role.DEFINITION)
        return null
    }

    override fun visitTypeReference(typeReference: KtTypeReference, u: Unit?): Symbol? {
        val symbol = super.visitTypeReference(typeReference, Unit)
        emitter.emitSemanticdbData(symbol!!, typeReference, Role.REFERENCE)
        return null
    }
}

@ExperimentalContracts
open class SymbolGenVisitor(
    private val resolver: DescriptorResolver,
    private val globals: GlobalSymbolsCache,
    private val locals: LocalSymbolsCache
): KtVisitor<Symbol?, Unit?>() {
    override fun visitElement(element: PsiElement) = element.acceptChildren(this)

    override fun visitClass(klass: KtClass, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(klass)!!
        val symbol = globals[desc, locals]
        println("NAMED TYPE $klass ${desc.name} $symbol")
        super.visitClass(klass, Unit)
        return symbol
    }

    override fun visitNamedFunction(function: KtNamedFunction, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(function)!!
        val symbol = globals[desc, locals]
        println("NAMED FUN $function ${desc.name} $symbol")
        super.visitNamedFunction(function, Unit)
        return symbol
    }

    override fun visitProperty(property: KtProperty, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(property)!!
        val symbol = globals[desc, locals]
        println("NAMED PROP $property ${desc.name} $symbol")
        super.visitProperty(property, Unit)
        return symbol
    }

    override fun visitCallExpression(expression: KtCallExpression, u: Unit?): Symbol? {
        val desc = resolver.fromReference(expression.calleeExpression as KtReferenceExpression)!!
        val symbol = globals[desc, locals]
        println("CALL EXPR $expression ${desc.name} $symbol")
        super.visitCallExpression(expression, Unit)
        return symbol
    }

    override fun visitParameter(parameter: KtParameter, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(parameter)!!
        val symbol = globals[desc, locals]
        println("NAMED PARAM $parameter ${desc.name} $symbol")
        super.visitParameter(parameter, Unit)
        return symbol
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(parameter)!!
        val symbol = globals[desc, locals]
        println("NAMED TYPE-PARAM $parameter ${desc.name} $symbol")
        super.visitTypeParameter(parameter, Unit)
        return symbol
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, u: Unit?): Symbol? {
        val desc = resolver.fromDeclaration(typeAlias)!!
        val symbol = globals[desc, locals]
        println("NAMED TYPE-ALIAS $typeAlias ${desc.name} $symbol")
        super.visitTypeAlias(typeAlias, Unit)
        return symbol
    }

    override fun visitTypeReference(typeReference: KtTypeReference, u: Unit?): Symbol? {
        val type = resolver.fromTypeReference(typeReference).let {
            if (it.isNullable()) return@let it.makeNotNullable()
            else return@let it
        }
        val desc = if (!type.isTypeParameter()) {
            DescriptorUtils.getClassDescriptorForType(type)
        } else {
            TypeUtils.getTypeParameterDescriptorOrNull(type)!!
        }
        val symbol = globals[desc, locals]
        println("TYPE REFERENCE $typeReference $type ${desc.name} $symbol")
        super.visitTypeReference(typeReference, Unit)
        return symbol
    }
}