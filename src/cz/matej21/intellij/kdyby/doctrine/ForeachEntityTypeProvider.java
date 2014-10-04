package cz.matej21.intellij.kdyby.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ForeachStatement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeSignatureKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class ForeachEntityTypeProvider implements PhpTypeProvider2 {

	@Override
	public char getKey() {
		return '\u2102';
	}

	@Nullable
	@Override
	public String getType(PsiElement psiElement) {
		if (!(psiElement instanceof Variable)) {
			return null;
		}
		Variable variable = (Variable) psiElement;
		PsiElement arrayAccessExpression = variable.getParent();
		if (!(arrayAccessExpression instanceof ForeachStatement)) {
			return null;
		}
		ForeachStatement arrayIndex = (ForeachStatement) arrayAccessExpression;
		PsiElement operation = arrayIndex.getArray();
		if (!(operation instanceof PhpTypedElement)) {
			return null;
		}
		PhpType value = ((PhpTypedElement) operation).getType();
		if (variable == arrayIndex.getKey()) {
			return null;
		}

		if (variable != arrayIndex.getValue()) {
			return null;
		}
		for (String strType : value.elementType().getTypes()) {
			if (strType.length() < 2
					|| strType.charAt(0) != '#'
					|| strType.indexOf(RepositoryMethodTypeProvider.KEY) == -1
					|| !strType.contains("[]")) {
				continue;
			}
			if (PhpTypeSignatureKey.ARRAY_ELEMENT.is(strType.charAt(1))) {
				strType = strType.substring(2);
			}
			return strType.substring(strType.indexOf(RepositoryMethodTypeProvider.KEY) + 1, strType.indexOf("[]"));
		}
		return null;
	}

	@Override
	public Collection<? extends PhpNamedElement> getBySignature(String s, Project project) {
		return PhpIndex.getInstance(project).getAnyByFQN(s);
	}
}
