package cz.matej21.intellij.kdyby.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.visitors.PhpRecursiveElementVisitor;

import java.util.ArrayList;
import java.util.Collection;


public class ElementValueResolver {

	private final PsiElement element;

	public ElementValueResolver(PsiElement element) {
		super();
		this.element = element;
	}

	public String resolve() {
		try {
			return doResolve(this.element);
		} catch (UnresolvableValueException e) {
			return null;
		}
	}

	public static String resolve(PsiElement element) {
		return (new ElementValueResolver(element)).resolve();
	}


	private String doResolve(PsiElement element) throws UnresolvableValueException {
		if (element instanceof StringLiteralExpression) {
			return ((StringLiteralExpression) element).getContents();
		} else if (element instanceof BinaryExpression && element.getNode().getElementType().equals(PhpElementTypes.CONCATENATION_EXPRESSION)) {
			BinaryExpression binaryExpression = (BinaryExpression) element;

			return doResolve(binaryExpression.getLeftOperand()) + doResolve(binaryExpression.getRightOperand());
		} else if (element instanceof ClassConstantReference) {
			ClassConstantReference constantReference = (ClassConstantReference) element;
			ClassReference classReference = (ClassReference) constantReference.getClassReference();
			if (constantReference.getLastChild() instanceof LeafPsiElement) {
				String constantName = constantReference.getLastChild().getText();
				if (constantName.equals("class")) {
					return classReference.getFQN();
				}
				for (PhpClass phpClass : getClasses(classReference, element.getProject())) {
					Field constant = phpClass.findFieldByName(constantName, true);
					if (constant != null && constant.isConstant()) {
						try {
							return doResolve(constant.getDefaultValue());
						} catch (UnresolvableValueException e) {
						}
					}
				}
			}
		} else if (element instanceof MethodReference) {
			MethodReference methodReference = (MethodReference) element;
			ClassReference classReference = (ClassReference) methodReference.getClassReference();
			for (PhpClass phpClass : getClasses(classReference, element.getProject())) {
				Method method = phpClass.findMethodByName(methodReference.getName());
				if (method == null) {
					continue;
				}
				ReturnVisitor returnVisitor = new ReturnVisitor(classReference);
				method.accept(returnVisitor);
				if (returnVisitor.getResult() != null) {
					return returnVisitor.getResult();
				}

			}
		}
		throw new UnresolvableValueException();

	}

	private static Collection<PhpClass> getClasses(PhpTypedElement element, Project project) {
		PhpIndex phpIndex = PhpIndex.getInstance(project);
		Collection<PhpClass> classes = new ArrayList<PhpClass>();
		for (String className : element.getType().getTypes()) {
			classes.addAll(phpIndex.getClassesByFQN(className));
		}

		return classes;
	}

	private class UnresolvableValueException extends Exception {
	}

	private class ReturnVisitor extends PhpRecursiveElementVisitor {

		protected String result;

		protected ClassReference classReference;

		public ReturnVisitor(ClassReference classReference) {
			this.classReference = classReference;
		}

		@Override
		public void visitPhpReturn(PhpReturn returnStatement) {
			PsiElement el = returnStatement.getArgument();
			if (el instanceof FunctionReference) {
				if (((FunctionReference) el).getName().equals("get_called_class")) {
					result = classReference.getFQN();
				}
			}
		}

		public String getResult() {
			return result;
		}
	}
}

